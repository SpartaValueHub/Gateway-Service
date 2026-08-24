package com.sparta.gateway.filter;

import com.sparta.gateway.security.GatewayRequestTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Security/CORS 이후 Gateway 라우팅 진입. 이 로그가 없으면 WebFilter(CORS·JWT)에서 끊긴 것이다.
 */
@Component
public class RequestTraceGlobalFilter implements GlobalFilter, Ordered {

	private static final Logger log = LoggerFactory.getLogger(RequestTraceGlobalFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		GatewayRequestTrace.enter(log, "GatewayRouting", exchange);
		return chain.filter(exchange)
				.doOnError(error -> GatewayRequestTrace.fail(log, "GatewayRouting", exchange, error))
				.doFinally(signal -> GatewayRequestTrace.complete(log, "GatewayRouting", exchange));
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 2;
	}
}
