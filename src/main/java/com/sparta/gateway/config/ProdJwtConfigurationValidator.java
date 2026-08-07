package com.sparta.gateway.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * prod 프로필에서 JWT 비활성화를 fail-closed로 차단한다.
 * SECURITY_JWT_ENABLED=false 등으로 전 구간 permitAll이 되지 않도록 한다.
 */
@Component
@Profile("prod")
class ProdJwtConfigurationValidator {

	private static final String JWT_ENABLED_PROPERTY = "security.jwt.enabled";

	private final Environment environment;

	ProdJwtConfigurationValidator(Environment environment) {
		this.environment = environment;
	}

	@PostConstruct
	void validateProdJwtConfiguration() {
		Boolean enabled = environment.getProperty(JWT_ENABLED_PROPERTY, Boolean.class);
		if (enabled == null || !enabled) {
			throw new IllegalStateException(
					"prod profile requires security.jwt.enabled=true");
		}
	}
}
