package com.sparta.gateway.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicroserviceRouteConfig {

	private static final List<String> PROXY_SERVICES = List.of(
			"auth-service",
			"chat-service",
			"member-service",
			"category-service",
			"member-regions-service",
			"listing-service",
			"reviews-service",
			"reports-service",
			"notifications-service",
			"premium-plans-service",
			"bo-service",
			"reservations-service",
			"discovery-service"
	);

	@Bean
	public RouteLocator microserviceRouteLocator(RouteLocatorBuilder builder, GatewayOpenApiProperties properties) {
		RouteLocatorBuilder.Builder routes = builder.routes();

		List<String> services = new ArrayList<>(properties.services());
		PROXY_SERVICES.stream()
				.filter(service -> !services.contains(service))
				.forEach(services::add);

		services.forEach(service -> routes.route(
				service + "-proxy",
				r -> r.order(0)
						.path("/" + service + "/**")
						.filters(f -> f.rewritePath("/" + service + "/(?<segment>.*)", "/${segment}"))
						.uri("lb://" + service)));

		return routes.build();
	}

}
