package com.sparta.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import com.sparta.gateway.config.GatewayOpenApiProperties;

/** MSA Edge — CORS·JWT 검증·lb:// 라우팅. FE·Swagger는 :8000(Gateway) 단일 진입점 */
@EnableDiscoveryClient
@EnableConfigurationProperties(GatewayOpenApiProperties.class)
@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}
