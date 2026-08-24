package com.sparta.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GatewayCorsConfigTest {

	@Test
	void allowsProductionFrontendAndSwaggerOrigins() {
		assertThat(GatewayCorsConfig.DEFAULT_ALLOWED_ORIGIN_PATTERNS)
				.contains(
						"https://valuehub.art",
						"https://www.valuehub.art",
						"https://api.valuehub.art"
				);
	}
}
