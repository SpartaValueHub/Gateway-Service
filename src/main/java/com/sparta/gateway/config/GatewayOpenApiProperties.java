package com.sparta.gateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Swagger UI·RouteLocator 공통 서비스 목록 — application.yml gateway.openapi.services */
@ConfigurationProperties(prefix = "gateway.openapi")
public record GatewayOpenApiProperties(List<String> services) {

	public GatewayOpenApiProperties {
		services = services != null ? List.copyOf(services) : List.of();
	}
}
