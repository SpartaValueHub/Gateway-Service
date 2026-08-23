package com.sparta.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

import java.util.stream.Stream;

/**
 * Gateway Edge public path — JWT 검증 예외.
 * auth public API는 auth-service SecurityConfig 와 동일 경로를 유지.
 * member-service: 회원가입 전 닉네임 중복 확인·현재 유효 약관 조회만 public.
 */
public final class SecurityPathConstants {

	private SecurityPathConstants() {
	}

	public static final String[] AUTH_PUBLIC_PATHS = {
			"/*/api/v1/auth/sign-up/**",
			"/*/api/v1/auth/sign-in/**",
			"/*/api/v1/auth/refresh/**",
			"/*/api/v1/auth/check/**",
			// 타인 가입일 조회 (FO 프로필) — GET only downstream
			"/*/api/v1/auth/members/*/joined-at",
			// logout·기타 auth API는 JWT on 시 Bearer 필요
			"/*/api/v1/identity-verifications/**"
	};

	public static final String[] MEMBER_PUBLIC_PATHS = {
			"/*/api/v1/members/check/nickname",
			"/*/api/v1/terms/active",
	};

	// GET 조회만 비로그인 허용 (POST/PUT/DELETE는 JWT 필요)
	public static final String[] CATEGORY_PUBLIC_GET_PATHS = {
			"/*/api/v1/categories/**"
	};

	// GET 목록 + 상세 조회만 비로그인 허용
	public static final String[] PRODUCT_POST_PUBLIC_GET_PATHS = {
			"/*/api/v1/product-posts",
			"/*/api/v1/product-posts/*"
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
		// HEALTH + Swagger + auth/member public — JwtSecurityConfig·SecurityConfig 공통
		return Stream.of(INFRA_PUBLIC_PATHS, AUTH_PUBLIC_PATHS, MEMBER_PUBLIC_PATHS)
				.flatMap(Stream::of)
				.toArray(String[]::new);
	}

	/** JWT on public chain — auth API는 regex matcher, infra·member public은 pathMatchers */
	public static OrServerWebExchangeMatcher jwtPublicExchangeMatcher() {
		return new OrServerWebExchangeMatcher(
				new AuthPublicServerWebExchangeMatcher(),
				ServerWebExchangeMatchers.pathMatchers(INFRA_PUBLIC_PATHS),
				ServerWebExchangeMatchers.pathMatchers(MEMBER_PUBLIC_PATHS),
				ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, CATEGORY_PUBLIC_GET_PATHS),
				ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, PRODUCT_POST_PUBLIC_GET_PATHS)
		);
	}
}
