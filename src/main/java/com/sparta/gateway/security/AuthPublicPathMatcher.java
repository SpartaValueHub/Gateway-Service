package com.sparta.gateway.security;

import java.util.regex.Pattern;

/**
 * Gateway Edge public API — JWT on 시 permitAll 대상.
 * PathPattern 와일드카드 이슈 회피를 위해 URI path regex로 판별.
 * auth-service SecurityConfig public API 와 동기화.
 * member-service: 닉네임 중복 확인·유효 약관·타인 공개 프로필 public.
 * (Swagger·Gateway /health 는 {@link SecurityPathConstants#INFRA_PUBLIC_PATHS}).
 */
public final class AuthPublicPathMatcher {

	private static final Pattern AUTH_PUBLIC_API = Pattern.compile(
			"^/[^/]+/api/v1/auth/(sign-up(?:/resume)?|sign-in|refresh|check/(login-id|email))/?$"
	);

	// FO 판매자 프로필용 가입일 — Auth GET /auth/members/{memberUuid}/joined-at
	private static final Pattern AUTH_MEMBER_JOINED_AT_PUBLIC_API = Pattern.compile(
			"^/[^/]+/api/v1/auth/members/[^/]+/joined-at/?$"
	);

	private static final Pattern IDENTITY_PUBLIC_API = Pattern.compile(
			"^/[^/]+/api/v1/identity-verifications(?:/.*)?$"
	);

	private static final Pattern MEMBER_PUBLIC_API = Pattern.compile(
			"^/[^/]+/api/v1/members/check/nickname/?$"
	);

	// FO 판매자 프로필 — Member GET /members/{memberUuid}/profile
	private static final Pattern MEMBER_PROFILE_PUBLIC_API = Pattern.compile(
			"^/[^/]+/api/v1/members/[^/]+/profile/?$"
	);

	private static final Pattern MEMBER_TERMS_PUBLIC_API = Pattern.compile(
			"^/[^/]+/api/v1/terms/active/?$"
	);

	private AuthPublicPathMatcher() {
	}

	public static boolean isPublic(String path) {
		if (path == null || path.isBlank()) {
			return false;
		}
		String normalized = path.endsWith("/") && path.length() > 1
				? path.substring(0, path.length() - 1)
				: path;
		return AUTH_PUBLIC_API.matcher(normalized).matches()
				|| AUTH_MEMBER_JOINED_AT_PUBLIC_API.matcher(normalized).matches()
				|| IDENTITY_PUBLIC_API.matcher(normalized).matches()
				|| MEMBER_PUBLIC_API.matcher(normalized).matches()
				|| MEMBER_PROFILE_PUBLIC_API.matcher(normalized).matches()
				|| MEMBER_TERMS_PUBLIC_API.matcher(normalized).matches();
	}
}
