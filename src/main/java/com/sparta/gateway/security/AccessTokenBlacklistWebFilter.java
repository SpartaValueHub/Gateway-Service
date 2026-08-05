package com.sparta.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * JWT jti가 auth-service logout blacklist(Redis)에 있으면 401.
 */
public class AccessTokenBlacklistWebFilter implements WebFilter {

	private static final String KEY_PREFIX = "auth:blacklist:access:";

	private final ReactiveStringRedisTemplate redisTemplate;
	private final String accessCookieName;

	public AccessTokenBlacklistWebFilter(
			ReactiveStringRedisTemplate redisTemplate,
			@Value("${auth.cookie.access-name:vh_access_token}") String accessCookieName
	) {
		this.redisTemplate = redisTemplate;
		this.accessCookieName = accessCookieName;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (AuthPublicPathMatcher.isPublic(exchange.getRequest().getURI().getPath())) {
			return chain.filter(exchange);
		}

		return exchange.getPrincipal()
				.filter(JwtAuthenticationToken.class::isInstance)
				.cast(JwtAuthenticationToken.class)
				.flatMap(auth -> {
					String jti = auth.getToken().getId();
					if (jti == null || jti.isBlank()) {
						return chain.filter(exchange).then(Mono.empty());
					}
					return redisTemplate.hasKey(KEY_PREFIX + jti)
							.flatMap(blacklisted -> {
								if (Boolean.TRUE.equals(blacklisted)) {
									exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
									return exchange.getResponse().setComplete();
								}
								return chain.filter(exchange).then(Mono.empty());
							});
				})
				.switchIfEmpty(chain.filter(exchange));
	}
}
