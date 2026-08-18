package com.sparta.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ProdJwtConfigurationValidatorTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(ProdJwtConfigurationValidator.class)
			.withPropertyValues("spring.profiles.active=prod");

	@Test
	void prodStartsWhenJwtEnabled() {
		contextRunner
				.withPropertyValues("security.jwt.enabled=true")
				.run(context -> assertThat(context).hasNotFailed());
	}

	@Test
	void prodFailsWhenJwtDisabled() {
		contextRunner
				.withPropertyValues("security.jwt.enabled=false")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasRootCauseInstanceOf(IllegalStateException.class)
							.hasRootCauseMessage("prod profile requires security.jwt.enabled=true");
				});
	}

	@Test
	void prodFailsWhenJwtEnabledMissing() {
		contextRunner
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasRootCauseInstanceOf(IllegalStateException.class)
							.hasRootCauseMessage("prod profile requires security.jwt.enabled=true");
				});
	}

	@Test
	void nonProdProfileDoesNotLoadValidator() {
		new ApplicationContextRunner()
				.withUserConfiguration(ProdJwtConfigurationValidator.class)
				.withPropertyValues(
						"spring.profiles.active=local",
						"security.jwt.enabled=false"
				)
				.run(context -> assertThat(context).doesNotHaveBean(ProdJwtConfigurationValidator.class));
	}
}
