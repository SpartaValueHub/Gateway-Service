package com.sparta.gateway.security;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Gateway Edge 공통 Error Response — auth-service ErrorResponseVo와 동일 필드 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GatewayErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message,
		String path,
		Long retryAfterSeconds
) {
}
