package com.sparta.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class StripUntrustedForwardedHeadersFilterTest {

    private final StripUntrustedForwardedHeadersFilter filter = new StripUntrustedForwardedHeadersFilter();

    @Test
    void removesClientSuppliedForwardedHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth-service/api/v1/auth/sign-in")
                .remoteAddress(new InetSocketAddress("203.0.113.10", 443))
                .header("X-Forwarded-For", "203.0.113.99")
                .header("Forwarded", "for=203.0.113.99")
                .header("X-Forwarded-Proto", "https")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> {
            assertThat(ex.getRequest().getHeaders().getFirst("X-Forwarded-For")).isNull();
            assertThat(ex.getRequest().getHeaders().getFirst("Forwarded")).isNull();
            assertThat(ex.getRequest().getHeaders().getFirst("X-Forwarded-Proto")).isNull();
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void keepsAlbForwardedHeadersFromPrivateNetwork() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth-service/api/v1/auth/sign-in")
                .remoteAddress(new InetSocketAddress("172.31.6.10", 443))
                .header("X-Forwarded-For", "203.0.113.99")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "api.valuehub.art")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> {
            assertThat(ex.getRequest().getHeaders().getFirst("X-Forwarded-For")).isEqualTo("203.0.113.99");
            assertThat(ex.getRequest().getHeaders().getFirst("X-Forwarded-Proto")).isEqualTo("https");
            assertThat(ex.getRequest().getHeaders().getFirst("X-Forwarded-Host")).isEqualTo("api.valuehub.art");
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void preservesNonForwardedHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/auth-service/api/v1/auth/sign-in")
                .header("Origin", "http://localhost:3000")
                .header("X-Forwarded-For", "203.0.113.99")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> {
            assertThat(ex.getRequest().getHeaders().getFirst("Origin")).isEqualTo("http://localhost:3000");
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }
}
