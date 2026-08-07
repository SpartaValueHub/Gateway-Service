package com.sparta.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("local")
class MicroserviceRouteConfigTest {

	@Autowired
	private RouteLocator routeLocator;

	@Test
	void memberServiceRouteIsRegisteredWithLbUri() {
		StepVerifier.create(routeLocator.getRoutes()
						.filter(route -> "member-service-proxy".equals(route.getId()))
						.single())
				.assertNext(route -> {
					org.assertj.core.api.Assertions.assertThat(route.getUri().toString())
							.isEqualTo("lb://member-service");
				})
				.verifyComplete();
	}

	@Test
	void authAndChatServiceRoutesAreRegistered() {
		StepVerifier.create(routeLocator.getRoutes().map(Route::getId).collectList())
				.assertNext(ids -> org.assertj.core.api.Assertions.assertThat(ids)
						.contains("auth-service-proxy", "chat-service-proxy", "member-service-proxy"))
				.verifyComplete();
	}

}
