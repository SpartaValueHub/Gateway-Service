package com.sparta.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;

/**
 * CI/CD·로드밸런서 헬스체크 — deploy.yml curl /health.
 * SecurityPathConstants.HEALTH_PATHS — JWT on/off 모두 public.
 */
@Configuration
public class GatewayHealthRouterConfig {

	@Bean
	public RouterFunction<ServerResponse> gatewayHealthRouter() {
		return RouterFunctions.route()
				.GET("/", request -> ServerResponse.ok().bodyValue(Map.of(
						"service", "gateway-service",
						"status", "UP"
				)))
				.GET("/health", request -> ServerResponse.ok().bodyValue(Map.of("status", "UP")))
				.build();
	}
}
