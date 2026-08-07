package com.sparta.gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * JWT 명시적 off 전용 — public 외 전 구간 permitAll.
 * security.jwt.enabled=true(또는 미설정)이면 JwtSecurityConfig 가 활성화된다.
 */
@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "false", matchIfMissing = false)
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
