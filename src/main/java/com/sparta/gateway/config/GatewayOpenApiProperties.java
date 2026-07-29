package com.sparta.gateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.openapi")
public record GatewayOpenApiProperties(List<String> services) {

	public GatewayOpenApiProperties {
		services = services != null ? List.copyOf(services) : List.of();
	}
}
