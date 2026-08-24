package com.sparta.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class OpenApiServersRewriterTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void resolveGatewayServiceUrl_usesForwardedHeaders() {
		MockServerHttpRequest request = MockServerHttpRequest.get("http://gateway:8000/auth-service/v3/api-docs")
				.header("X-Forwarded-Proto", "https")
				.header("X-Forwarded-Host", "api.example.com")
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		String url = OpenApiServersRewriter.resolveGatewayServiceUrl(exchange, "auth-service");

		assertEquals("https://api.example.com/auth-service", url);
	}

	@Test
	void resolveGatewayServiceUrl_usesProductionApiHost() {
		MockServerHttpRequest request = MockServerHttpRequest.get("http://gateway:8000/auth-service/v3/api-docs")
				.header("X-Forwarded-Proto", "https")
				.header("X-Forwarded-Host", "api.valuehub.art")
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		String url = OpenApiServersRewriter.resolveGatewayServiceUrl(exchange, "auth-service");

		assertEquals("https://api.valuehub.art/auth-service", url);
	}

	@Test
	void resolveGatewayServiceUrl_fallsBackToHostHeader() {
		MockServerHttpRequest request = MockServerHttpRequest.get("http://54.116.150.139:8000/auth-service/v3/api-docs")
				.header(HttpHeaders.HOST, "54.116.150.139:8000")
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		String url = OpenApiServersRewriter.resolveGatewayServiceUrl(exchange, "chat-service");

		assertEquals("http://54.116.150.139:8000/chat-service", url);
	}

	@Test
	void rewriteServers_replacesDockerInternalUrl() throws Exception {
		String body = """
				{"openapi":"3.1.0","servers":[{"url":"http://172.18.0.8:8081","description":"Generated server url"}],"paths":{}}
				""";

		String rewritten = OpenApiServersRewriter.rewriteServers(
				objectMapper,
				body,
				"http://54.116.150.139:8000/auth-service");

		JsonNode servers = objectMapper.readTree(rewritten).get("servers");
		assertEquals(1, servers.size());
		assertEquals("http://54.116.150.139:8000/auth-service", servers.get(0).get("url").asText());
		assertTrue(servers.get(0).get("description").asText().contains("Gateway"));
	}
}
