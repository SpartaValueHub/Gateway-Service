package com.sparta.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

class CookieBearerTokenAuthenticationConverterTest {

	private final CookieBearerTokenAuthenticationConverter converter =
			new CookieBearerTokenAuthenticationConverter("vh_access_token");

	@Test
	void convertsBearerAuthorizationHeader() {
		var exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/auth-service/api/v1/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer header-token")
						.build()
		);

		var auth = converter.convert(exchange).block();

		assertThat(auth).isInstanceOf(BearerTokenAuthenticationToken.class);
		assertThat(((BearerTokenAuthenticationToken) auth).getToken()).isEqualTo("header-token");
	}

	@Test
	void convertsAccessTokenCookieWhenHeaderMissing() {
		var exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/auth-service/api/v1/auth/logout")
						.cookie(new HttpCookie("vh_access_token", "cookie-token"))
						.build()
		);

		var auth = converter.convert(exchange).block();

		assertThat(auth).isInstanceOf(BearerTokenAuthenticationToken.class);
		assertThat(((BearerTokenAuthenticationToken) auth).getToken()).isEqualTo("cookie-token");
	}

	@Test
	void prefersAuthorizationHeaderOverCookie() {
		var exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/auth-service/api/v1/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer header-token")
						.cookie(new HttpCookie("vh_access_token", "cookie-token"))
						.build()
		);

		var auth = converter.convert(exchange).block();

		assertThat(((BearerTokenAuthenticationToken) auth).getToken()).isEqualTo("header-token");
	}

	@Test
	void returnsEmptyWhenNoTokenPresent() {
		var exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/auth-service/api/v1/auth/logout").build()
		);

		var auth = converter.convert(exchange).block();

		assertThat(auth).isNull();
	}
}
