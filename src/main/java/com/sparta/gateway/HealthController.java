package com.sparta.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

	@GetMapping("/")
	public Map<String, String> root() {
		return Map.of(
				"service", "gateway-service",
				"status", "UP"
		);
	}

	@GetMapping("/health")
	public Map<String, String> health() {
		return Map.of("status", "UP");
	}
}
