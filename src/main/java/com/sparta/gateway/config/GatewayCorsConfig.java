package com.sparta.gateway.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import com.sparta.gateway.security.GatewayRequestTrace;

import reactor.core.publisher.Mono;

/**
 * 브라우저 → Gateway 직접 호출용 CORS.
 * MSA 정책상 downstream(auth-service 등)에는 CORS를 두지 않음.
 */
@Configuration
public class GatewayCorsConfig {

	private static final Logger log = LoggerFactory.getLogger(GatewayCorsConfig.class);

	/** 브라우저·Swagger Origin. credentials=true 라 * 불가, 패턴만 허용 */
	static final List<String> DEFAULT_ALLOWED_ORIGIN_PATTERNS = List.of(
			"http://localhost:3000",
			"http://127.0.0.1:3000",
			"http://192.168.10.45:3000",
			"http://192.168.*.*:3000",
			"https://valuehub-fe.vercel.app",
			"https://www.valuehub.art",
			"https://valuehub.art",
			"https://api.valuehub.art"
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
		return new CorsWebFilter(source) {
			@Override
			public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
				GatewayRequestTrace.enter(log, "Cors", exchange);
				return super.filter(exchange, chain)
						.doOnError(error -> GatewayRequestTrace.fail(log, "Cors", exchange, error))
						.doFinally(signal -> GatewayRequestTrace.complete(log, "Cors", exchange));
			}
		};
	}

}
