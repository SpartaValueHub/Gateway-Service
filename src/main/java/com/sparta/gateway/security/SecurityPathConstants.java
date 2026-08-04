package com.sparta.gateway.security;

/**
 * Gateway Edge public path — JWT 검증 예외.
 * auth public API는 auth-service SecurityConfig 와 동일 경로를 유지.
 */
public final class SecurityPathConstants {

	private SecurityPathConstants() {
	}

	public static final String[] HEALTH_PATHS = {
			"/",
			"/health",
			"/health/**"
	};

	public static final String[] SWAGGER_UI_PATHS = {
			"/swagger-ui.html",
			"/swagger-ui/**"
	};

	public static final String[] API_DOCS_PATHS = {
			"/v3/api-docs",
			"/v3/api-docs/**",
			"/webjars/**"
	};

	public static final String[] MICROSERVICE_API_DOCS_PATHS = {
			"/*/v3/api-docs",
			"/*/v3/api-docs/**"
	};

	public static final String[] AUTH_PUBLIC_PATHS = {
			"/*/api/v1/auth/sign-up",
			"/*/api/v1/auth/sign-in",
			"/*/api/v1/auth/refresh",
			"/*/api/v1/auth/check/**",
			// logout·기타 auth API는 JWT on 시 Bearer 필요
			"/*/api/v1/identity-verifications/**"
	};

	public static String[] publicPaths() {
		// HEALTH + Swagger + auth public — JwtSecurityConfig·SecurityConfig 공통
		return new String[] {
				"/",
				"/health",
				"/health/**",
				"/swagger-ui.html",
				"/swagger-ui/**",
				"/v3/api-docs",
				"/v3/api-docs/**",
				"/webjars/**",
				"/*/v3/api-docs",
				"/*/v3/api-docs/**",
				"/*/api/v1/auth/sign-up",
				"/*/api/v1/auth/sign-in",
				"/*/api/v1/auth/refresh",
				"/*/api/v1/auth/check/**",
				"/*/api/v1/identity-verifications/**"
		};
	}
}
