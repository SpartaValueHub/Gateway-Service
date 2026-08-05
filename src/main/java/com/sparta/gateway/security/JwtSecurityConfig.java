package com.sparta.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * SECURITY_JWT_ENABLED=true — public API는 JWT 필터 없이 permitAll, 나머지만 JWT 검증.
 * Access token은 Authorization Bearer 또는 HttpOnly Cookie(vh_access_token).
 */
@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
public class JwtSecurityConfig {

	@Bean
	@Order(0)
	public SecurityWebFilterChain publicSecurityWebFilterChain(ServerHttpSecurity http) {
		return http
				.securityMatcher(SecurityPathConstants.jwtPublicExchangeMatcher())
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchange -> exchange.anyExchange().permitAll())
				.build();
	}

	@Bean
	@Order(1)
	public SecurityWebFilterChain jwtSecurityWebFilterChain(
			ServerHttpSecurity http,
			ReactiveJwtDecoder reactiveJwtDecoder,
			CookieBearerTokenAuthenticationConverter cookieBearerTokenAuthenticationConverter,
			AccessTokenBlacklistWebFilter accessTokenBlacklistWebFilter,
			InternalAuthHeaderWebFilter internalAuthHeaderWebFilter
	) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchange -> exchange.anyExchange().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.bearerTokenConverter(cookieBearerTokenAuthenticationConverter)
						.jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder)))
				.addFilterAfter(accessTokenBlacklistWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
				.addFilterAfter(internalAuthHeaderWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
				.build();
	}

	@Bean
	public ReactiveJwtDecoder reactiveJwtDecoder(
			@Value("${jwt.public-key:}") String publicKeyPem,
			@Value("${jwt.public-key-location:}") String publicKeyLocation,
			ResourceLoader resourceLoader
	) {
		String pem = resolvePublicKeyPem(publicKeyPem, publicKeyLocation, resourceLoader);
		RSAPublicKey publicKey = parsePublicKey(pem);
		return NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
	}

	@Bean
	public AccessTokenBlacklistWebFilter accessTokenBlacklistWebFilter(
			ReactiveStringRedisTemplate redisTemplate,
			@Value("${auth.dependency-failure.retry-after-seconds:5}") long dependencyRetryAfterSeconds
	) {
		return new AccessTokenBlacklistWebFilter(redisTemplate, dependencyRetryAfterSeconds);
	}

	@Bean
	public InternalAuthHeaderWebFilter internalAuthHeaderWebFilter() {
		return new InternalAuthHeaderWebFilter();
	}

	private static String resolvePublicKeyPem(
			String inlinePem,
			String location,
			ResourceLoader resourceLoader
	) {
		if (StringUtils.hasText(inlinePem)) {
			return inlinePem;
		}
		if (StringUtils.hasText(location)) {
			try {
				return new String(
						resourceLoader.getResource(location).getInputStream().readAllBytes(),
						StandardCharsets.UTF_8
				);
			} catch (Exception ex) {
				throw new IllegalStateException("Failed to load JWT public key from " + location, ex);
			}
		}
		throw new IllegalStateException("jwt.public-key or jwt.public-key-location must be configured");
	}

	private static RSAPublicKey parsePublicKey(String pem) {
		try {
			String sanitized = pem
					.replace("-----BEGIN PUBLIC KEY-----", "")
					.replace("-----END PUBLIC KEY-----", "")
					.replaceAll("\\s", "");
			byte[] decoded = Base64.getDecoder().decode(sanitized);
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
			return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
		} catch (Exception ex) {
			throw new IllegalStateException("Invalid JWT public key PEM", ex);
		}
	}
}
