package com.sparta.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * WebFilter 체인 진입·완료. 이 로그가 없으면 요청이 Gateway JVM에 도달하지 않은 것이다 (ALB/WAF 등).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceWebFilter implements WebFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestTraceWebFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		GatewayRequestTrace.enter(log, "RequestTrace", exchange);
		return chain.filter(exchange)
				.doOnError(error -> GatewayRequestTrace.fail(log, "RequestTrace", exchange, error))
				.doFinally(signal -> GatewayRequestTrace.complete(log, "RequestTrace", exchange));
	}
}
