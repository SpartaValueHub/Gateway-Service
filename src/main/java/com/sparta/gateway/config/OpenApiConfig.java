package com.sparta.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Gateway Swagger UI — downstream /{service}/v3/api-docs 집약, Bearer는 auth-service RS256 access */
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI gatewayOpenAPI() {
		final String securitySchemeName = "Bearer Authentication";

		return new OpenAPI()
				.info(new Info()
						.title("MSA API Gateway")
						.description("Gateway를 통한 Microservice API 문서")
						.version("1.0"))
				.addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
				.components(new Components()
						.addSecuritySchemes(securitySchemeName, new SecurityScheme()
								.name(securitySchemeName)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("JWT Access Token (Auth Service 구현 후 발급)")));
	}
}
