package com.sparta.gateway.security;

public final class SecurityPathConstants {

	private SecurityPathConstants() {
	}

	public static final String[] HEALTH_PATHS = {
			"/",
			"/health"
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

	public static String[] publicPaths() {
		return new String[] {
				"/",
				"/health",
				"/swagger-ui.html",
				"/swagger-ui/**",
				"/v3/api-docs",
				"/v3/api-docs/**",
				"/webjars/**",
				"/*/v3/api-docs",
				"/*/v3/api-docs/**"
		};
	}
}
