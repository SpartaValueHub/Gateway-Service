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

	@GetMapping("/member")
	public Mono<Map<String, Object>> memberViaGateway() {
		return callService("member-service", "/health/test");
	}

	@GetMapping("/category")
	public Mono<Map<String, Object>> categoryViaGateway() {
		return callService("category-service", "/health/test");
	}

	@GetMapping("/member-regions")
	public Mono<Map<String, Object>> memberRegionsViaGateway() {
		return callService("member-regions-service", "/health/test");
	}

	@GetMapping("/listing")
	public Mono<Map<String, Object>> listingViaGateway() {
		return callService("listing-service", "/health/test");
	}

	@GetMapping("/reviews")
	public Mono<Map<String, Object>> reviewsViaGateway() {
		return callService("reviews-service", "/health/test");
	}

	@GetMapping("/reports")
	public Mono<Map<String, Object>> reportsViaGateway() {
		return callService("reports-service", "/health/test");
	}

	@GetMapping("/notifications")
	public Mono<Map<String, Object>> notificationsViaGateway() {
		return callService("notifications-service", "/health/test");
	}

	@GetMapping("/premium-plans")
	public Mono<Map<String, Object>> premiumPlansViaGateway() {
		return callService("premium-plans-service", "/health/test");
	}

	@GetMapping("/bo")
	public Mono<Map<String, Object>> boViaGateway() {
		return callService("bo-service", "/health/test");
	}

	@GetMapping("/discovery")
	public Mono<Map<String, Object>> discoveryViaGateway() {
		return callService("discovery-service", "/health/test");
	}

	@GetMapping("/all")
	public Mono<Map<String, Object>> all() {
		return Mono.zip(
				Mono.zip(
						callService("auth-service", "/health/test").onErrorReturn(errorResult("auth-service")),
						callService("chat-service", "/health/test").onErrorReturn(errorResult("chat-service")),
						callService("member-service", "/health/test").onErrorReturn(errorResult("member-service")),
						callService("category-service", "/health/test").onErrorReturn(errorResult("category-service")),
						callService("member-regions-service", "/health/test").onErrorReturn(errorResult("member-regions-service")),
						callService("listing-service", "/health/test").onErrorReturn(errorResult("listing-service")),
						callService("reviews-service", "/health/test").onErrorReturn(errorResult("reviews-service"))
				),
				Mono.zip(
						callService("reports-service", "/health/test").onErrorReturn(errorResult("reports-service")),
						callService("discovery-service", "/health/test").onErrorReturn(errorResult("discovery-service")),
						callService("notifications-service", "/health/test").onErrorReturn(errorResult("notifications-service")),
						callService("premium-plans-service", "/health/test").onErrorReturn(errorResult("premium-plans-service")),
						callService("bo-service", "/health/test").onErrorReturn(errorResult("bo-service"))
				)
		).map(tuple -> {
			Map<String, Object> result = new LinkedHashMap<>();
			result.put("gateway", Map.of("service", "gateway-service", "status", "UP"));
			result.put("auth-service", tuple.getT1().getT1());
			result.put("chat-service", tuple.getT1().getT2());
			result.put("member-service", tuple.getT1().getT3());
			result.put("category-service", tuple.getT1().getT4());
			result.put("member-regions-service", tuple.getT1().getT5());
			result.put("listing-service", tuple.getT1().getT6());
			result.put("reviews-service", tuple.getT1().getT7());
			result.put("reports-service", tuple.getT2().getT1());
			result.put("discovery-service", tuple.getT2().getT2());
			result.put("notifications-service", tuple.getT2().getT3());
			result.put("premium-plans-service", tuple.getT2().getT4());
			result.put("bo-service", tuple.getT2().getT5());
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
