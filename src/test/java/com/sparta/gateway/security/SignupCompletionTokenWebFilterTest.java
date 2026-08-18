package com.sparta.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignupCompletionTokenWebFilterTest {

	private static final String CREATE_PATH = "/member-service/api/v1/members";
	private static final String KEY = "auth:signup-completion:member-uuid-1";

	@Mock
	private ReactiveStringRedisTemplate redisTemplate;

	@Mock
	private ReactiveValueOperations<String, String> valueOperations;

	@Mock
	private WebFilterChain chain;

	private SignupCompletionTokenWebFilter filter;

	@BeforeEach
	void setUp() {
		filter = new SignupCompletionTokenWebFilter(redisTemplate);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.execute(any(RedisScript.class), anyList(), anyList())).thenReturn(Flux.just(1L));
	}

	@Test
	void accessTokenIsNotTouchedByCompletionTokenFilter() {
		MockServerWebExchange exchange = postExchange(CREATE_PATH, accessToken());

		StepVerifier.create(filter.filter(exchange, respondWith(exchange, HttpStatus.CREATED)))
				.verifyComplete();

		verify(valueOperations, never()).get(anyString());
		verify(chain).filter(exchange);
	}

	@Test
	void completionTokenOnMemberCreateIsForwarded() {
		MockServerWebExchange exchange = postExchange(CREATE_PATH, completionToken("jti-1"));
		when(valueOperations.get(KEY)).thenReturn(Mono.just("jti-1"));

		StepVerifier.create(filter.filter(exchange, respondWith(exchange, HttpStatus.CREATED)))
				.verifyComplete();

		verify(chain).filter(exchange);
	}

	@Test
	void completionTokenIsConsumedOnlyAfterSuccessfulResponse() {
		MockServerWebExchange exchange = postExchange(CREATE_PATH, completionToken("jti-1"));
		when(valueOperations.get(KEY)).thenReturn(Mono.just("jti-1"));

		filter.filter(exchange, respondWith(exchange, HttpStatus.CREATED)).block();

		verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(KEY)), eq(List.of("jti-1")));
	}

	/**
	 * downstream 실패 시 토큰을 소비하면 재시도 불가능한 부분 가입이 남는다.
	 * 2xx 가 아니면 토큰을 유지해 재시도를 허용해야 한다.
	 */
	@Test
	void completionTokenSurvivesDownstreamFailureSoRetryIsPossible() {
		MockServerWebExchange exchange = postExchange(CREATE_PATH, completionToken("jti-1"));
		when(valueOperations.get(KEY)).thenReturn(Mono.just("jti-1"));

		filter.filter(exchange, respondWith(exchange, HttpStatus.GATEWAY_TIMEOUT)).block();

		verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), anyList());
	}

	@Test
	void downstreamExceptionIsNotMisclassifiedAsSecurityStoreFailure() {
		MockServerWebExchange exchange = postExchange(CREATE_PATH, completionToken("jti-1"));
		when(valueOperations.get(KEY)).thenReturn(Mono.just("jti-1"));
		IllegalStateException downstreamFailure = new IllegalStateException("member-service unavailable");
		when(chain.filter(exchange)).thenReturn(Mono.error(downstreamFailure));

		StepVerifier.create(filter.filter(exchange, chain))
				.expectErrorMatches(error -> error == downstreamFailure)
				.verify();

		assertThat(exchange.getResponse().getStatusCode()).isNull();
	}

	@Test
	void redisLookupFailureIsReportedAsSecurityStoreUnavailable() {
		MockServerWebExchange exchange = postExchange(CREATE_PATH, completionToken("jti-1"));
		when(valueOperations.get(KEY)).thenReturn(Mono.error(new IllegalStateException("redis unavailable")));

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		verify(chain, never()).filter(any());
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	void completionTokenIsRejectedOnOtherEndpoints() {
		MockServerWebExchange exchange = MockServerWebExchange
				.builder(MockServerHttpRequest.get("/member-service/api/v1/members/me").build())
				.principal(new JwtAuthenticationToken(completionToken("jti-1")))
				.build();

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		verify(chain, never()).filter(any());
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void unknownJtiIsRejected() {
		MockServerWebExchange exchange = postExchange(CREATE_PATH, completionToken("jti-1"));
		when(valueOperations.get(KEY)).thenReturn(Mono.empty());

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		verify(chain, never()).filter(any());
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void rotatedJtiIsRejected() {
		MockServerWebExchange exchange = postExchange(CREATE_PATH, completionToken("jti-1"));
		when(valueOperations.get(KEY)).thenReturn(Mono.just("jti-2"));

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		verify(chain, never()).filter(any());
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	/** downstream 이 응답 본문을 쓰는 시점에 beforeCommit 훅이 실행된다. */
	private WebFilterChain respondWith(MockServerWebExchange exchange, HttpStatus status) {
		doAnswer(invocation -> {
			exchange.getResponse().setStatusCode(status);
			return exchange.getResponse().writeWith(
					Mono.just(exchange.getResponse().bufferFactory().wrap(new byte[0]))
			);
		}).when(chain).filter(any());
		return chain;
	}

	private static MockServerWebExchange postExchange(String path, Jwt jwt) {
		return MockServerWebExchange
				.builder(MockServerHttpRequest.post(path).build())
				.principal(new JwtAuthenticationToken(jwt))
				.build();
	}

	private static Jwt completionToken(String jti) {
		return jwt(Map.of(
				"sub", "member-uuid-1",
				"jti", jti,
				"tokenType", "SIGNUP_COMPLETION",
				"purpose", "MEMBER_PROFILE_CREATE"
		));
	}

	private static Jwt accessToken() {
		return jwt(Map.of("sub", "member-uuid-1", "jti", "jti-access", "tokenType", "access"));
	}

	private static Jwt jwt(Map<String, Object> claims) {
		Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
		return new Jwt("token", issuedAt, issuedAt.plusSeconds(120), Map.of("alg", "RS256"), claims);
	}
}
