package com.sparta.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.buffer.DataBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * JWT jti가 auth-service logout blacklist(Redis)에 있으면 401.
 * Redis 장애 정책: fail-closed — protected JWT route에서 503 AUTH_SECURITY_STORE_UNAVAILABLE.
 * public auth route는 Redis 장애와 무관하게 통과.
 */
public class AccessTokenBlacklistWebFilter implements WebFilter {

	private static final Logger log = LoggerFactory.getLogger(AccessTokenBlacklistWebFilter.class);
	private static final String KEY_PREFIX = "auth:blacklist:access:";
	private static final String DEPENDENCY_UNAVAILABLE_MESSAGE =
			"인증 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

	private final org.springframework.data.redis.core.ReactiveStringRedisTemplate redisTemplate;
	private final long dependencyRetryAfterSeconds;

	public AccessTokenBlacklistWebFilter(
			org.springframework.data.redis.core.ReactiveStringRedisTemplate redisTemplate,
			long dependencyRetryAfterSeconds
	) {
		this.redisTemplate = redisTemplate;
		this.dependencyRetryAfterSeconds = dependencyRetryAfterSeconds;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		GatewayRequestTrace.enter(log, "AccessTokenBlacklist", exchange);
		if (AuthPublicPathMatcher.isPublic(exchange.getRequest().getURI().getPath())) {
			GatewayRequestTrace.skip(log, "AccessTokenBlacklist", exchange, "public-path");
			return chain.filter(exchange);
		}

		return exchange.getPrincipal()
				.ofType(JwtAuthenticationToken.class)
				.flatMap(auth -> processJwt(exchange, chain, auth).thenReturn(Boolean.TRUE))
				.switchIfEmpty(Mono.defer(() -> {
					GatewayRequestTrace.skip(log, "AccessTokenBlacklist", exchange, "no-jwt");
					return chain.filter(exchange).thenReturn(Boolean.FALSE);
				}))
				.then();
	}

	private Mono<Void> processJwt(ServerWebExchange exchange, WebFilterChain chain, JwtAuthenticationToken auth) {
		String tokenType = auth.getToken().getClaimAsString("tokenType");
		if (tokenType != null && !"access".equals(tokenType)) {
			GatewayRequestTrace.skip(log, "AccessTokenBlacklist", exchange, "non-access-token");
			return chain.filter(exchange);
		}
		String jti = auth.getToken().getId();
		if (jti == null || jti.isBlank()) {
			GatewayRequestTrace.skip(log, "AccessTokenBlacklist", exchange, "no-jti");
			return chain.filter(exchange);
		}
		return redisTemplate.hasKey(KEY_PREFIX + jti)
				.flatMap(blacklisted -> {
					if (Boolean.TRUE.equals(blacklisted)) {
						GatewayRequestTrace.reject(log, "AccessTokenBlacklist", exchange,
								HttpStatus.UNAUTHORIZED, "blacklisted-jti");
						exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
						return exchange.getResponse().setComplete();
					}
					GatewayRequestTrace.pass(log, "AccessTokenBlacklist", exchange);
					return chain.filter(exchange);
				})
				.onErrorResume(ex -> {
					GatewayRequestTrace.fail(log, "AccessTokenBlacklist", exchange, ex);
					return writeSecurityStoreUnavailable(exchange);
				});
	}

	private Mono<Void> writeSecurityStoreUnavailable(ServerWebExchange exchange) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
		response.getHeaders().add(HttpHeaders.RETRY_AFTER, String.valueOf(dependencyRetryAfterSeconds));
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		return writeJsonBody(response, new GatewayErrorResponse(
				Instant.now(),
				HttpStatus.SERVICE_UNAVAILABLE.value(),
				"AUTH_SECURITY_STORE_UNAVAILABLE",
				DEPENDENCY_UNAVAILABLE_MESSAGE,
				exchange.getRequest().getURI().getPath(),
				dependencyRetryAfterSeconds
		));
	}

	private Mono<Void> writeJsonBody(ServerHttpResponse response, GatewayErrorResponse body) {
		try {
			byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(body);
			DataBuffer buffer = response.bufferFactory().wrap(bytes);
			return response.writeWith(Mono.just(buffer)).then();
		} catch (Exception ex) {
			return response.setComplete();
		}
	}
}
