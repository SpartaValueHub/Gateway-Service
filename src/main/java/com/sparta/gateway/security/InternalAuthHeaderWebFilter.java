package com.sparta.gateway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * JWT 검증 성공 시 downstream에 memberUuid·role 내부 헤더 전달.
 */
public class InternalAuthHeaderWebFilter implements WebFilter {

	public static final String MEMBER_UUID_HEADER = "X-Member-Uuid";
	public static final String ROLE_HEADER = "X-Role";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return exchange.getPrincipal()
				.filter(JwtAuthenticationToken.class::isInstance)
				.cast(JwtAuthenticationToken.class)
				.flatMap(auth -> {
					String memberUuid = auth.getToken().getSubject();
					String role = auth.getToken().getClaimAsString("role");
					if (role == null || role.isBlank()) {
						role = "USER";
					}

					ServerHttpRequest mutated = exchange.getRequest().mutate()
							.header(MEMBER_UUID_HEADER, memberUuid)
							.header(ROLE_HEADER, role)
							.build();
					return chain.filter(exchange.mutate().request(mutated).build());
				})
				.switchIfEmpty(chain.filter(exchange));
	}
}
