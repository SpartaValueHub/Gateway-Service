package com.sparta.gateway.security;

import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** JWT public path — AuthPublicPathMatcher 기반 */
public class AuthPublicServerWebExchangeMatcher implements ServerWebExchangeMatcher {

	@Override
	public Mono<MatchResult> matches(ServerWebExchange exchange) {
		String path = exchange.getRequest().getPath().pathWithinApplication().value();
		if (AuthPublicPathMatcher.isPublic(path)) {
			return MatchResult.match();
		}
		return MatchResult.notMatch();
	}
}
