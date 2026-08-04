package com.sparta.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** CI/CD·로드밸런서 헬스체크 — deploy.yml curl /health */
@RestController
public class HealthController {

	@GetMapping("/")
	public Map<String, String> root() {
		return Map.of(
				"service", "gateway-service",
				"status", "UP"
		);
	}

	/** SecurityPathConstants.HEALTH_PATHS — JWT on/off 모두 public */
	@GetMapping("/health")
	public Map<String, String> health() {
		return Map.of("status", "UP");
	}
}
