package com.sparta.gateway.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPublicPathMatcherTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"/auth-service/api/v1/auth/sign-up",
			"/auth-service/api/v1/auth/sign-in",
			"/auth-service/api/v1/auth/refresh",
			"/auth-service/api/v1/auth/check/login-id",
			"/auth-service/api/v1/auth/check/email",
			"/auth-service/api/v1/identity-verifications/confirm",
			"/auth-service/api/v1/identity-verifications/identity-verification-001"
	})
	void isPublic_allowsAuthAndIdentityPublicApis(String path) {
		assertThat(AuthPublicPathMatcher.isPublic(path)).isTrue();
	}

	@Test
	void isPublic_deniesProtectedAuthApis() {
		assertThat(AuthPublicPathMatcher.isPublic("/auth-service/api/v1/auth/logout")).isFalse();
	}

}
