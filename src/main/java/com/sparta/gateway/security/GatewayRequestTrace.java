package com.sparta.gateway.security;

import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;

/**
 * Gateway 필터 통과 위치 추적용 로그. 비밀번호·Authorization·Cookie는 남기지 않는다.
 */
public final class GatewayRequestTrace {

	static final String PREFIX = "[gw-trace]";

	private GatewayRequestTrace() {
	}

	public static void enter(Logger log, String filterName, ServerWebExchange exchange) {
		if (log.isInfoEnabled()) {
			log.info("{} enter filter={} {}", PREFIX, filterName, describe(exchange));
		}
	}

	public static void skip(Logger log, String filterName, ServerWebExchange exchange, String reason) {
		if (log.isInfoEnabled()) {
			log.info("{} skip filter={} reason={} {}", PREFIX, filterName, reason, describe(exchange));
		}
	}

	public static void pass(Logger log, String filterName, ServerWebExchange exchange) {
		if (log.isInfoEnabled()) {
			log.info("{} pass filter={} {}", PREFIX, filterName, describe(exchange));
		}
	}

	public static void reject(Logger log, String filterName, ServerWebExchange exchange, HttpStatusCode status, String reason) {
		log.warn("{} reject filter={} status={} reason={} {}", PREFIX, filterName, status, reason, describe(exchange));
	}

	public static void fail(Logger log, String filterName, ServerWebExchange exchange, Throwable error) {
		log.warn("{} fail filter={} error={} {}", PREFIX, filterName, error.toString(), describe(exchange));
	}

	public static void complete(Logger log, String filterName, ServerWebExchange exchange) {
		if (log.isInfoEnabled()) {
			log.info("{} complete filter={} status={} {}",
					PREFIX, filterName, exchange.getResponse().getStatusCode(), describe(exchange));
		}
	}

	static String describe(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		HttpHeaders headers = request.getHeaders();
		InetSocketAddress remote = request.getRemoteAddress();
		String path = request.getURI().getRawPath();
		return "method=" + request.getMethod()
				+ " path=" + path
				+ " host=" + headers.getFirst(HttpHeaders.HOST)
				+ " origin=" + headers.getFirst(HttpHeaders.ORIGIN)
				+ " forwardedProto=" + headers.getFirst("X-Forwarded-Proto")
				+ " forwardedHost=" + headers.getFirst("X-Forwarded-Host")
				+ " remote=" + (remote == null ? null : remote.getAddress())
				+ " public=" + AuthPublicPathMatcher.isPublic(path);
	}
}
