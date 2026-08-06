package com.sparta.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.buffer.DataBuffer;
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

	private static final String KEY_PREFIX = "auth:blacklist:access:";
	private static final String SESSION_TERMINATED_CODE = "AUTH_SESSION_TERMINATED";
	private static final String SESSION_TERMINATED_MESSAGE =
			"다른 기기에서 로그인하여 현재 세션이 종료되었습니다.";
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
		if (AuthPublicPathMatcher.isPublic(exchange.getRequest().getURI().getPath())) {
			return chain.filter(exchange);
		}

		return exchange.getPrincipal()
				.ofType(JwtAuthenticationToken.class)
				.flatMap(auth -> processJwt(exchange, chain, auth).thenReturn(Boolean.TRUE))
				.switchIfEmpty(Mono.defer(() -> chain.filter(exchange).thenReturn(Boolean.FALSE)))
				.then();
	}

	private Mono<Void> processJwt(ServerWebExchange exchange, WebFilterChain chain, JwtAuthenticationToken auth) {
		String jti = auth.getToken().getId();
		if (jti == null || jti.isBlank()) {
			return chain.filter(exchange);
		}
		return redisTemplate.hasKey(KEY_PREFIX + jti)
				.flatMap(blacklisted -> {
					if (Boolean.TRUE.equals(blacklisted)) {
						return writeSessionTerminated(exchange);
					}
					return chain.filter(exchange);
				})
				.onErrorResume(ex -> writeSecurityStoreUnavailable(exchange));
	}

	private Mono<Void> writeSessionTerminated(ServerWebExchange exchange) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		return writeJsonBody(response, new GatewayErrorResponse(
				Instant.now(),
				HttpStatus.UNAUTHORIZED.value(),
				SESSION_TERMINATED_CODE,
				SESSION_TERMINATED_MESSAGE,
				exchange.getRequest().getURI().getPath(),
				null
		));
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
