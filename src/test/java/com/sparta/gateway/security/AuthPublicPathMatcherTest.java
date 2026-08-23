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
			"/auth-service/api/v1/auth/members/550e8400-e29b-41d4-a716-446655440000/joined-at",
			"/member-service/api/v1/members/check/nickname",
			"/member-service/api/v1/terms/active",
			"/auth-service/api/v1/identity-verifications/confirm",
			"/auth-service/api/v1/identity-verifications/identity-verification-001"
	})
	void isPublic_allowsAuthAndIdentityPublicApis(String path) {
		assertThat(AuthPublicPathMatcher.isPublic(path)).isTrue();
	}

	@Test
	void isPublic_deniesProtectedAuthApis() {
		assertThat(AuthPublicPathMatcher.isPublic("/auth-service/api/v1/auth/logout")).isFalse();
		assertThat(AuthPublicPathMatcher.isPublic("/auth-service/api/v1/auth/me")).isFalse();
		assertThat(AuthPublicPathMatcher.isPublic("/auth-service/api/v1/auth/members/550e8400-e29b-41d4-a716-446655440000")).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"/member-service/api/v1/members/me",
			"/member-service/api/v1/members",
			"/member-service/api/v1/terms",
			"/member-service/health",
			"/chat-service/api/v1/chat/rooms"
	})
	void isPublic_deniesProtectedMicroserviceApis(String path) {
		assertThat(AuthPublicPathMatcher.isPublic(path)).isFalse();
	}

}
