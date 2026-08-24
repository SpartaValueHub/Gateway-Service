package com.sparta.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * 인터넷에서 직접 온 Forwarded / X-Forwarded-* 는 제거한다.
 * VPC 사설망(ALB)에서 온 헤더는 유지한다 — ALB가 붙인 {@code X-Forwarded-Proto=https} 를
 * 지우면 Gateway가 TCP(HTTP) 기준으로 다시 써서 Secure 쿠키·Swagger URL이 깨진다.
 * 미적용 시 공개 :8000 으로 클라이언트 X-Forwarded-For spoofing → auth-service rate limit 우회 가능.
 */
@Component
public class StripUntrustedForwardedHeadersFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(StripUntrustedForwardedHeadersFilter.class);

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
        GatewayRequestTrace.enter(log, "StripForwardedHeaders", exchange);
        boolean trusted = isTrustedProxy(exchange.getRequest().getRemoteAddress());
        if (trusted) {
            GatewayRequestTrace.skip(log, "StripForwardedHeaders", exchange, "trusted-proxy-keep-forwarded");
            return chain.filter(exchange)
                    .doOnError(error -> GatewayRequestTrace.fail(log, "StripForwardedHeaders", exchange, error));
        }
        GatewayRequestTrace.skip(log, "StripForwardedHeaders", exchange, "untrusted-strip-forwarded");
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        builder.headers(headers -> HEADERS_TO_STRIP.forEach(headers::remove));
        return chain.filter(exchange.mutate().request(builder.build()).build())
                .doOnError(error -> GatewayRequestTrace.fail(log, "StripForwardedHeaders", exchange, error));
    }

    static boolean isTrustedProxy(InetSocketAddress remote) {
        if (remote == null) {
            return false;
        }
        InetAddress address = remote.getAddress();
        return address != null && address.isSiteLocalAddress();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
