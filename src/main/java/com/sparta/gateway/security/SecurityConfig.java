package com.sparta.gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 로컬·초기 연동용 — JWT off 시 public 외 전 구간 permitAll.
 * SECURITY_JWT_ENABLED=true 이면 JwtSecurityConfig 가 대체(@ConditionalOnProperty).
 */
@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "false", matchIfMissing = true)
public class SecurityConfig {

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchange -> exchange
						.matchers(new AuthPublicServerWebExchangeMatcher()).permitAll()
						.pathMatchers(SecurityPathConstants.publicPaths()).permitAll()
						// JWT off: public 외도 permitAll — 로컬·초기 FE 연동용
						.anyExchange().permitAll())
				.build();
	}
}
