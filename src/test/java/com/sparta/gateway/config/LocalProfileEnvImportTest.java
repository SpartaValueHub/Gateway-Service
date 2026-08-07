package com.sparta.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * local 프로필이 .env 를 Spring 설정으로 가져오는지, 그리고 JWT 기본값이 on 인지 검증한다.
 */
class LocalProfileEnvImportTest {

	@Test
	void localProfileImportsEnvFileIntoSpringConfig() throws IOException {
		List<PropertySource<?>> sources = new YamlPropertySourceLoader()
				.load("application-local", new ClassPathResource("application-local.yml"));

		assertThat(sources).isNotEmpty();
		assertThat(sources.get(0).getProperty("spring.config.import"))
				.isEqualTo("optional:file:.env[.properties]");
	}

	@Test
	void jwtEnabledFlagDefaultsToTrueWhenEnvVariableMissing() throws IOException {
		List<PropertySource<?>> sources = new YamlPropertySourceLoader()
				.load("application", new ClassPathResource("application.yml"));

		assertThat(sources).isNotEmpty();
		assertThat(sources.get(0).getProperty("security.jwt.enabled"))
				.isEqualTo("${SECURITY_JWT_ENABLED:true}");
	}
}
