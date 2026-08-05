package com.sparta.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Authorization Bearer 우선, 없으면 access token HttpOnly Cookie에서 추출.
 */
@Component
public class CookieBearerTokenAuthenticationConverter implements ServerAuthenticationConverter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final String accessCookieName;

	public CookieBearerTokenAuthenticationConverter(
			@Value("${auth.cookie.access-name:vh_access_token}") String accessCookieName
	) {
		this.accessCookieName = accessCookieName;
	}

	@Override
	public Mono<Authentication> convert(ServerWebExchange exchange) {
		String token = resolveFromAuthorizationHeader(exchange);
		if (!StringUtils.hasText(token)) {
			token = resolveFromCookie(exchange);
		}
		if (!StringUtils.hasText(token)) {
			return Mono.empty();
		}
		return Mono.just(new BearerTokenAuthenticationToken(token));
	}

	private String resolveFromAuthorizationHeader(ServerWebExchange exchange) {
		String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
			return authorization.substring(BEARER_PREFIX.length()).trim();
		}
		return null;
	}

	private String resolveFromCookie(ServerWebExchange exchange) {
		var cookie = exchange.getRequest().getCookies().getFirst(accessCookieName);
		return cookie != null ? cookie.getValue() : null;
	}
}
