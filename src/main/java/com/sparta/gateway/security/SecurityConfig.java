package com.sparta.gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * JWT 명시적 off 전용(local/test만) — public 외 전 구간 permitAll.
 * 기본·prod 등에서는 JwtSecurityConfig 가 활성화된다(matchIfMissing=true).
 * prod 에서 JWT off 는 ProdJwtConfigurationValidator 가 기동을 실패시킨다.
 */
@Configuration
@EnableWebFluxSecurity
@Profile({"local", "test"})
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "false", matchIfMissing = false)
public class SecurityConfig {

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchange -> exchange
						.matchers(new AuthPublicServerWebExchangeMatcher()).permitAll()
						.pathMatchers(SecurityPathConstants.publicPaths()).permitAll()
						// JWT off: public 외도 permitAll — local/test 전용
						.anyExchange().permitAll())
				.build();
	}
}

