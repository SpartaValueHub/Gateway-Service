package com.sparta.gateway.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 브라우저 → Gateway 직접 호출용 CORS.
 * MSA 정책상 downstream(auth-service 등)에는 CORS를 두지 않음.
 */
@Configuration
public class GatewayCorsConfig {

	/** FO dev — LAN IP 패턴은 팀 로컬·동일 대역 테스트용 */
	private static final List<String> DEFAULT_ALLOWED_ORIGIN_PATTERNS = List.of(
			"http://localhost:3000",
			"http://127.0.0.1:3000",
			"http://192.168.10.45:3000",
			"http://192.168.*.*:3000"
	);

	@Bean
	public CorsWebFilter corsWebFilter() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(DEFAULT_ALLOWED_ORIGIN_PATTERNS);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
		config.setAllowedHeaders(List.of("*"));
		config.setExposedHeaders(List.of("*"));
		// FE httpOnly cookie — credentials: include, Authorization Bearer 불필요
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return new CorsWebFilter(source);
	}

}
