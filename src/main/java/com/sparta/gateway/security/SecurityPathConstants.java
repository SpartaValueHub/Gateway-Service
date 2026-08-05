package com.sparta.gateway.security;

import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

import java.util.stream.Stream;

/**
 * Gateway Edge public path — JWT 검증 예외.
 * auth public API는 auth-service SecurityConfig 와 동일 경로를 유지.
 */
public final class SecurityPathConstants {

	private SecurityPathConstants() {
	}

	public static final String[] AUTH_PUBLIC_PATHS = {
			"/*/api/v1/auth/sign-up/**",
			"/*/api/v1/auth/sign-in/**",
			"/*/api/v1/auth/refresh/**",
			"/*/api/v1/auth/check/**",
			// logout·기타 auth API는 JWT on 시 Bearer 필요
			"/*/api/v1/identity-verifications/**"
	};

	public static final String[] INFRA_PUBLIC_PATHS = {
			"/",
			"/health",
			"/health/**",
			"/swagger-ui.html",
			"/swagger-ui/**",
			"/v3/api-docs",
			"/v3/api-docs/**",
			"/webjars/**",
			"/*/v3/api-docs",
			"/*/v3/api-docs/**"
	};

	public static String[] publicPaths() {
		// HEALTH + Swagger + auth public — JwtSecurityConfig·SecurityConfig 공통
		return Stream.of(INFRA_PUBLIC_PATHS, AUTH_PUBLIC_PATHS)
				.flatMap(Stream::of)
				.toArray(String[]::new);
	}

	/** JWT on public chain — auth API는 regex matcher, infra는 pathMatchers */
	public static OrServerWebExchangeMatcher jwtPublicExchangeMatcher() {
		return new OrServerWebExchangeMatcher(
				new AuthPublicServerWebExchangeMatcher(),
				ServerWebExchangeMatchers.pathMatchers(INFRA_PUBLIC_PATHS)
		);
	}
}
