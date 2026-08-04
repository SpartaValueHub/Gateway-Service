package com.sparta.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
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
 * SECURITY_JWT_ENABLED=true 일 때 활성 — auth-service RS256 public key로 Edge JWT 검증.
 * public path는 SecurityPathConstants 와 auth-service SecurityConfig 와 동기화.
 */
@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
public class JwtSecurityConfig {

	@Bean
	public SecurityWebFilterChain jwtSecurityWebFilterChain(
			ServerHttpSecurity http,
			ReactiveJwtDecoder reactiveJwtDecoder
	) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchange -> exchange
						.pathMatchers(SecurityPathConstants.publicPaths()).permitAll()
						// JWT on: logout 등 protected API는 Bearer 필수
						.anyExchange().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder)))
				.build();
	}

	@Bean
	public ReactiveJwtDecoder reactiveJwtDecoder(
			@Value("${jwt.public-key:}") String publicKeyPem,
			@Value("${jwt.public-key-location:}") String publicKeyLocation,
			ResourceLoader resourceLoader
	) {
		// auth-service jwt-public.pem — Private Key는 Gateway에 두지 않음
		String pem = resolvePublicKeyPem(publicKeyPem, publicKeyLocation, resourceLoader);
		RSAPublicKey publicKey = parsePublicKey(pem);
		return NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
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
