package com.sparta.gateway.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPublicPathMatcherTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"/auth-service/api/v1/auth/sign-up",
			"/auth-service/api/v1/auth/sign-up/resume",
			"/auth-service/api/v1/auth/sign-in",
			"/auth-service/api/v1/auth/refresh",
			"/auth-service/api/v1/auth/check/login-id",
			"/auth-service/api/v1/auth/check/email",
			"/member-service/api/v1/members/check/nickname",
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

	@ParameterizedTest
	@ValueSource(strings = {
			"/member-service/api/v1/members/me",
			"/member-service/api/v1/members",
			"/member-service/health",
			"/chat-service/api/v1/chat/rooms"
	})
	void isPublic_deniesProtectedMicroserviceApis(String path) {
		assertThat(AuthPublicPathMatcher.isPublic(path)).isFalse();
	}

}
