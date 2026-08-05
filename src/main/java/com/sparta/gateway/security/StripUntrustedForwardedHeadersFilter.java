package com.sparta.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 클라이언트가 보낸 Forwarded / X-Forwarded-* 를 제거한 뒤 downstream으로 전달.
 * Spring Cloud Gateway {@code ForwardedHeadersFilter}가 연결 remote address 기준으로 헤더를 재설정한다.
 * 미적용 시 클라이언트 X-Forwarded-For spoofing → auth-service rate limit 우회 가능.
 */
@Component
public class StripUntrustedForwardedHeadersFilter implements GlobalFilter, Ordered {

    static final List<String> HEADERS_TO_STRIP = List.of(
            "Forwarded",
            "X-Forwarded-For",
            "X-Forwarded-Host",
            "X-Forwarded-Proto",
            "X-Forwarded-Port",
            "X-Forwarded-Prefix"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        builder.headers(headers -> HEADERS_TO_STRIP.forEach(headers::remove));
        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
