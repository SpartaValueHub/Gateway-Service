package com.sparta.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.time.Instant;

/** 가입 완료 토큰을 member 생성 한 경로로 제한하고 성공 응답 뒤 원자적으로 소비한다. */
public class SignupCompletionTokenWebFilter implements WebFilter {
    private static final String TOKEN_TYPE = "SIGNUP_COMPLETION";
    private static final String PURPOSE = "MEMBER_PROFILE_CREATE";
    private static final String CREATE_PATH = "/member-service/api/v1/members";
    private static final String KEY_PREFIX = "auth:signup-completion:";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final RedisScript<Long> COMPARE_DELETE = RedisScript.of("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final ReactiveStringRedisTemplate redisTemplate;

    public SignupCompletionTokenWebFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // filterAuthenticated 는 Mono<Void>(항상 empty) 이므로 thenReturn 없이는
        // switchIfEmpty 가 인증된 요청에서도 downstream chain 을 한 번 더 구독한다.
        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .flatMap(auth -> filterAuthenticated(exchange, chain, auth).thenReturn(Boolean.TRUE))
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange).thenReturn(Boolean.FALSE)))
                .then();
    }

    private Mono<Void> filterAuthenticated(
            ServerWebExchange exchange,
            WebFilterChain chain,
            JwtAuthenticationToken auth
    ) {
        String tokenType = auth.getToken().getClaimAsString("tokenType");
        if (!TOKEN_TYPE.equals(tokenType)) {
            return chain.filter(exchange);
        }

        String purpose = auth.getToken().getClaimAsString("purpose");
        String path = normalize(exchange.getRequest().getURI().getPath());
        if (!PURPOSE.equals(purpose)
                || exchange.getRequest().getMethod() != HttpMethod.POST
                || !CREATE_PATH.equals(path)) {
            return writeError(exchange, HttpStatus.FORBIDDEN,
                    "SIGNUP_COMPLETION_TOKEN_INVALID", "유효하지 않은 가입 완료 토큰입니다.");
        }

        String subject = auth.getToken().getSubject();
        String jti = auth.getToken().getId();
        if (subject == null || subject.isBlank() || jti == null || jti.isBlank()) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED,
                    "SIGNUP_COMPLETION_TOKEN_INVALID", "유효하지 않은 가입 완료 토큰입니다.");
        }

        String key = KEY_PREFIX + subject;
        return redisTemplate.opsForValue().get(key)
                .onErrorMap(SignupCompletionSecurityStoreException::new)
                .flatMap(activeJti -> {
                    if (!jti.equals(activeJti)) {
                        return writeError(exchange, HttpStatus.UNAUTHORIZED,
                                "SIGNUP_COMPLETION_TOKEN_INVALID", "유효하지 않은 가입 완료 토큰입니다.");
                    }
                    exchange.getResponse().beforeCommit(() -> {
                        HttpStatus status = HttpStatus.resolve(exchange.getResponse().getStatusCode() == null
                                ? 200 : exchange.getResponse().getStatusCode().value());
                        if (status == null || !status.is2xxSuccessful()) {
                            return Mono.empty();
                        }
                        return redisTemplate.execute(COMPARE_DELETE, List.of(key), List.of(jti)).then();
                    });
                    return chain.filter(exchange);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    return writeError(exchange, HttpStatus.UNAUTHORIZED,
                            "SIGNUP_COMPLETION_TOKEN_INVALID", "유효하지 않은 가입 완료 토큰입니다.");
                }))
                .onErrorResume(SignupCompletionSecurityStoreException.class, ex -> {
                    return writeError(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                            "AUTH_SECURITY_STORE_UNAVAILABLE",
                            "인증 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.");
                });
    }

    private static final class SignupCompletionSecurityStoreException extends RuntimeException {
        private SignupCompletionSecurityStoreException(Throwable cause) {
            super(cause);
        }
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        if (exchange.getResponse().isCommitted()) return Mono.empty();
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(new GatewayErrorResponse(
                    Instant.now(), status.value(), code, message,
                    exchange.getRequest().getURI().getPath(), null));
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception ignored) {
            return exchange.getResponse().setComplete();
        }
    }

    private static String normalize(String path) {
        return path != null && path.length() > 1 && path.endsWith("/")
                ? path.substring(0, path.length() - 1) : path;
    }
}
