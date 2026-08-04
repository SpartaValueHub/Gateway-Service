package com.sparta.gateway.security;

import java.util.regex.Pattern;

/**
 * Gateway Edge public API — JWT on 시 permitAll 대상.
 * PathPattern 와일드카드 이슈 회피를 위해 URI path regex로 판별.
 * auth-service SecurityConfig public API 와 동기화.
 */
public final class AuthPublicPathMatcher {

	private static final Pattern AUTH_PUBLIC_API = Pattern.compile(
			"^/[^/]+/api/v1/auth/(sign-up|sign-in|refresh|check/(login-id|email))/?$"
	);

	private static final Pattern IDENTITY_PUBLIC_API = Pattern.compile(
			"^/[^/]+/api/v1/identity-verifications(?:/.*)?$"
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
				|| IDENTITY_PUBLIC_API.matcher(normalized).matches();
	}
}
