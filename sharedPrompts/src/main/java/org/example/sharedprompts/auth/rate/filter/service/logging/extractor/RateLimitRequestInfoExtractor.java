package org.example.sharedprompts.auth.rate.filter.service.logging.extractor;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.dto.admin.request.RateLimitRequestInfo;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.HttpRequestUtils;
import org.springframework.http.HttpMethod;

/**
 * Rate Limit 로그를 위한 HTTP 요청 정보 추출기
 * 
 * Rate Limit 로그 저장에 필요한 HTTP 요청 정보를 추출합니다.
 * RateLimitLoggingService의 책임을 분리하여 단일 책임 원칙을 준수합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RateLimitRequestInfoExtractor {

    /**
     * HTTP 요청에서 로그 저장에 필요한 정보를 추출합니다.
     * 
     * @param request HttpServletRequest
     * @return RateLimitRequestInfo
     * @throws ApiException HTTP Method가 유효하지 않거나 request가 null인 경우
     */
    public static RateLimitRequestInfo extract(HttpServletRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST_INFO, "request");
        }
        
        String uri = request.getRequestURI();
        String httpMethod = extractHttpMethod(request);
        String clientIp = HttpRequestUtils.getClientIpAddress(request);
        
        return new RateLimitRequestInfo(uri, httpMethod, clientIp);
    }

    /**
     * HTTP Method를 안전하게 추출합니다.
     * 
     * @param request HttpServletRequest
     * @return HTTP Method 문자열
     * @throws ApiException HTTP Method가 유효하지 않은 경우
     */
    private static String extractHttpMethod(HttpServletRequest request) {
        try {
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            return method.name();
        } catch (IllegalArgumentException e) {
            // 유효하지 않은 HTTP Method인 경우 원본 문자열 반환
            String method = request.getMethod();
            if (method == null || method.isBlank()) {
                throw new ApiException(ErrorCode.INVALID_REQUEST_INFO, "httpMethod");
            }
            return method.toUpperCase();
        }
    }
}

