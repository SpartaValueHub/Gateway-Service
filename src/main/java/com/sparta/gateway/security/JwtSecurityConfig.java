package com.sparta.gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Auth Service 구현 후 JWT Resource Server(RSA 공개키 검증) 설정을 이 클래스에 추가합니다.
 *
 * <p>활성화: {@code SECURITY_JWT_ENABLED=true} 환경변수 설정
 *
 * <p>예상 작업:
 * <ul>
 *   <li>{@code spring-boot-starter-oauth2-resource-server} 의존성 추가</li>
 *   <li>{@code JWT_PUBLIC_KEY} 또는 {@code JWT_PUBLIC_KEY_LOCATION} 으로 RSA 공개키 로드</li>
 *   <li>{@code SecurityWebFilterChain} 에 {@code oauth2ResourceServer().jwt()} 적용</li>
 *   <li>Swagger/API Docs 외 API 경로에 {@code authenticated()} 적용</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
public class JwtSecurityConfig {
}
