package com.sparta.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiDocsRouteConfig {

	@Bean
	public RouteLocator apiDocsRouteLocator(RouteLocatorBuilder builder, GatewayOpenApiProperties properties) {
		RouteLocatorBuilder.Builder routes = builder.routes();

		properties.services().forEach(service -> routes.route(
				service + "-api-docs",
				r -> r.order(0)
						.path("/" + service + "/v3/api-docs", "/" + service + "/v3/api-docs/**")
						.filters(f -> f.rewritePath("/" + service + "/(?<segment>.*)", "/${segment}"))
						.uri("lb://" + service)));

		return routes.build();
	}
}
