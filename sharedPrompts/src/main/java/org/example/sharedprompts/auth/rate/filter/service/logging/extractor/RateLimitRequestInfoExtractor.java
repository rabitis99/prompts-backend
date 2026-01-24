package org.example.sharedprompts.auth.rate.filter.service.logging.extractor;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.dto.admin.request.RateLimitRequestInfo;
import org.example.sharedprompts.global.util.HttpRequestUtils;
import org.springframework.http.HttpMethod;

/**
 * Rate Limit 로그를 위한 HTTP 요청 정보 추출기
 * 
 * Rate Limit 로그 저장에 필요한 HTTP 요청 정보를 추출합니다.
 * RateLimitLoggingService의 책임을 분리하여 단일 책임 원칙을 준수합니다.
 * 
 * fail-open 정책을 고려하여 검증 실패 시 기본값을 사용합니다.
 * 로깅 파이프라인에서 예외가 전파되어 fail-open 흐름을 방해하지 않도록 합니다.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RateLimitRequestInfoExtractor {

    private static final String DEFAULT_URI = "/unknown";
    private static final String DEFAULT_HTTP_METHOD = "UNKNOWN";
    private static final String DEFAULT_CLIENT_IP = "unknown";

    /**
     * HTTP 요청에서 로그 저장에 필요한 정보를 추출합니다.
     * 
     * fail-open 정책을 고려하여 검증 실패 시 기본값을 사용합니다.
     * 예외를 던지지 않아 로깅 파이프라인에서 fail-open 흐름을 방해하지 않습니다.
     * 
     * @param request HttpServletRequest
     * @return RateLimitRequestInfo (검증 실패 시 기본값 사용)
     */
    public static RateLimitRequestInfo extract(HttpServletRequest request) {
        if (request == null) {
            log.warn("RateLimitRequestInfoExtractor: request is null, using default values");
            return new RateLimitRequestInfo(DEFAULT_URI, DEFAULT_HTTP_METHOD, DEFAULT_CLIENT_IP);
        }
        
        String uri = extractUri(request);
        String httpMethod = extractHttpMethod(request);
        String clientIp = HttpRequestUtils.getClientIpAddress(request);
        
        // clientIp가 null이거나 비어있는 경우 기본값 사용
        if (clientIp == null || clientIp.isBlank()) {
            log.warn("RateLimitRequestInfoExtractor: clientIp is null or blank, using default value");
            clientIp = DEFAULT_CLIENT_IP;
        }
        
        return new RateLimitRequestInfo(uri, httpMethod, clientIp);
    }

    /**
     * URI를 안전하게 추출합니다.
     * 
     * @param request HttpServletRequest
     * @return URI 문자열 (null이거나 비어있는 경우 기본값)
     */
    private static String extractUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            log.warn("RateLimitRequestInfoExtractor: uri is null or blank, using default value");
            return DEFAULT_URI;
        }
        return uri;
    }

    /**
     * HTTP Method를 안전하게 추출합니다.
     * 
     * @param request HttpServletRequest
     * @return HTTP Method 문자열 (유효하지 않은 경우 기본값)
     */
    private static String extractHttpMethod(HttpServletRequest request) {
        try {
            String method = request.getMethod();
            if (method == null || method.isBlank()) {
                log.warn("RateLimitRequestInfoExtractor: httpMethod is null or blank, using default value");
                return DEFAULT_HTTP_METHOD;
            }
            
            // 유효한 HTTP Method인지 확인
            HttpMethod.valueOf(method);
            return method.toUpperCase();
        } catch (IllegalArgumentException e) {
            // 유효하지 않은 HTTP Method인 경우 기본값 사용
            log.warn("RateLimitRequestInfoExtractor: invalid httpMethod '{}', using default value", 
                    request.getMethod(), e);
            return DEFAULT_HTTP_METHOD;
        }
    }
}

