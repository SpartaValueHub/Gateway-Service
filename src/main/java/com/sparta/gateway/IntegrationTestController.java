package com.sparta.gateway;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class IntegrationTestController {

	private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
			new ParameterizedTypeReference<>() {
			};

	private final WebClient webClient;

	public IntegrationTestController(WebClient.Builder loadBalancedWebClientBuilder) {
		this.webClient = loadBalancedWebClientBuilder.build();
	}

	@GetMapping("/gateway")
	public Mono<Map<String, String>> gateway() {
		return Mono.just(Map.of(
				"service", "gateway-service",
				"status", "UP"
		));
	}

	@GetMapping("/auth")
	public Mono<Map<String, Object>> authViaGateway() {
		return callService("auth-service", "/health/test");
	}

	@GetMapping("/chat")
	public Mono<Map<String, Object>> chatViaGateway() {
		return callService("chat-service", "/health/test");
	}

	@GetMapping("/discovery")
	public Mono<Map<String, Object>> discoveryViaGateway() {
		return callService("discovery-service", "/health/test");
	}

	@GetMapping("/all")
	public Mono<Map<String, Object>> all() {
		return Mono.zip(
				callService("auth-service", "/health/test").onErrorReturn(errorResult("auth-service")),
				callService("chat-service", "/health/test").onErrorReturn(errorResult("chat-service")),
				callService("discovery-service", "/health/test").onErrorReturn(errorResult("discovery-service"))
		).map(tuple -> {
			Map<String, Object> result = new LinkedHashMap<>();
			result.put("gateway", Map.of("service", "gateway-service", "status", "UP"));
			result.put("auth-service", tuple.getT1());
			result.put("chat-service", tuple.getT2());
			result.put("discovery-service", tuple.getT3());
			return result;
		});
	}

	private Mono<Map<String, Object>> callService(String serviceId, String path) {
		return webClient.get()
				.uri("http://" + serviceId + path)
				.retrieve()
				.bodyToMono(MAP_TYPE)
				.map(body -> {
					Map<String, Object> result = new LinkedHashMap<>();
					result.put("routedVia", "gateway-service");
					result.put("target", serviceId);
					result.put("status", "UP");
					result.put("response", body);
					return result;
				})
				.onErrorResume(ex -> Mono.just(errorResult(serviceId, ex.getMessage())));
	}

	private Map<String, Object> errorResult(String serviceId) {
		return errorResult(serviceId, "unavailable");
	}

	private Map<String, Object> errorResult(String serviceId, String message) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("routedVia", "gateway-service");
		result.put("target", serviceId);
		result.put("status", "DOWN");
		result.put("message", message);
		return result;
	}

}
