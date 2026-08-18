package com.sparta.gateway.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Configuration
public class MicroserviceRouteConfig {

	private static final Logger log = LoggerFactory.getLogger(MicroserviceRouteConfig.class);

	private static final List<String> PROXY_SERVICES = List.of(
			"auth-service",
			"chat-service",
			"member-service",
			"category-service",
			"member-regions-service",
			"product-post-service",
			"reviews-service",
			"reports-service",
			"notifications-service",
			"premium-plans-service",
			"bo-service",
			"reservations-service",
			"discovery-service"
	);

	@Bean
	public RouteLocator microserviceRouteLocator(
			RouteLocatorBuilder builder,
			GatewayOpenApiProperties properties,
			ObjectMapper objectMapper) {
		RouteLocatorBuilder.Builder routes = builder.routes();

		List<String> services = new ArrayList<>(properties.services());
		PROXY_SERVICES.stream()
				.filter(service -> !services.contains(service))
				.forEach(services::add);

		services.forEach(service -> routes.route(
				service + "-proxy",
				r -> r.order(0)
						.path("/" + service + "/**")
						.filters(f -> f
								.filter((exchange, chain) -> {
									String path = exchange.getRequest().getURI().getRawPath();
									if (path != null && path.endsWith("/v3/api-docs")) {
										exchange.getAttributes().put(OpenApiServersRewriter.REWRITE_ATTR, Boolean.TRUE);
									}
									return chain.filter(exchange);
								})
								.rewritePath("/" + service + "/(?<segment>.*)", "/${segment}")
								.modifyResponseBody(String.class, String.class, (exchange, body) -> {
									if (!Boolean.TRUE.equals(exchange.getAttribute(OpenApiServersRewriter.REWRITE_ATTR))
											|| !StringUtils.hasText(body)) {
										return Mono.justOrEmpty(body);
									}
									try {
										String serverUrl = OpenApiServersRewriter.resolveGatewayServiceUrl(exchange, service);
										return Mono.just(OpenApiServersRewriter.rewriteServers(objectMapper, body, serverUrl));
									}
									catch (Exception ex) {
										log.warn("Failed to rewrite OpenAPI servers for {}: {}", service, ex.toString());
										return Mono.just(body);
									}
								}))
						.uri("lb://" + service)));

		return routes.build();
	}

}
