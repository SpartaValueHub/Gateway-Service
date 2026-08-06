package com.sparta.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessTokenBlacklistWebFilterTest {

    private static final long RETRY_AFTER_SECONDS = 5L;

    @Mock
    private org.springframework.data.redis.core.ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private WebFilterChain chain;

    private AccessTokenBlacklistWebFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AccessTokenBlacklistWebFilter(redisTemplate, RETRY_AFTER_SECONDS);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void mockExchangePrincipalIsVisibleToFilter() {
        Jwt jwt = jwtWithId("jti-001");
        MockServerWebExchange exchange = exchangeWithPrincipal(
                "/auth-service/api/v1/chat/rooms",
                new JwtAuthenticationToken(jwt)
        );

        StepVerifier.create(exchange.getPrincipal())
                .assertNext(principal -> {
                    assertThat(principal).isInstanceOf(JwtAuthenticationToken.class);
                    assertThat(((JwtAuthenticationToken) principal).getToken().getId()).isEqualTo("jti-001");
                })
                .verifyComplete();
    }

    @Test
    void publicAuthRouteBypassesBlacklistEvenWhenRedisWouldFail() {
        MockServerWebExchange exchange = exchangeForPath("/auth-service/api/v1/auth/sign-in");

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(redisTemplate, never()).hasKey(any());
        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void publicIdentityRouteBypassesBlacklist() {
        MockServerWebExchange exchange = exchangeForPath("/auth-service/api/v1/identity-verifications/status");

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(redisTemplate, never()).hasKey(any());
        verify(chain).filter(exchange);
    }

    @Test
    void unauthenticatedProtectedRouteDelegatesToChainWithoutRedis() {
        MockServerWebExchange exchange = exchangeForPath("/auth-service/api/v1/auth/logout");

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(redisTemplate, never()).hasKey(any());
        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void blacklistedAccessTokenReturns401WithSessionTerminatedBody() {
        Jwt jwt = jwtWithId("blacklisted-jti");
        MockServerWebExchange exchange = exchangeWithPrincipal(
                "/auth-service/api/v1/chat/rooms",
                new JwtAuthenticationToken(jwt)
        );
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(redisTemplate).hasKey("auth:blacklist:access:blacklisted-jti");
        verify(chain, never()).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).isNotNull();
        assertThat(body)
                .contains("\"code\":\"AUTH_SESSION_TERMINATED\"")
                .contains("다른 기기에서 로그인하여 현재 세션이 종료되었습니다.");
    }

    private static Jwt jwtWithId(String jti) {
        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        return new Jwt(
                "token",
                issuedAt,
                issuedAt.plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of("sub", "user", "jti", jti)
        );
    }

    private static MockServerWebExchange exchangeForPath(String path) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get(path).build()
        );
    }

    private static MockServerWebExchange exchangeWithPrincipal(
            String path,
            JwtAuthenticationToken authentication
    ) {
        return MockServerWebExchange.builder(MockServerHttpRequest.get(path).build())
                .principal(authentication)
                .build();
    }
}
