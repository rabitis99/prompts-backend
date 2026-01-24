package org.example.sharedprompts.dto.admin.request;

import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

/**
 * Rate Limit 로그를 위한 HTTP 요청 정보 DTO
 * 
 * HTTP 요청에서 추출한 정보를 담는 불변 레코드입니다.
 * 
 * @param uri 요청 URI
 * @param httpMethod HTTP Method (예: GET, POST)
 * @param clientIp 클라이언트 IP 주소
 */
public record RateLimitRequestInfo(
        String uri,
        String httpMethod,
        String clientIp
) {
    /**
     * 컴팩트 생성자: 필드 검증 수행
     * 
     * @throws ApiException 필드가 null이거나 비어있는 경우
     */
    public RateLimitRequestInfo {
        if (uri == null || uri.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST_INFO, "uri");
        }
        if (httpMethod == null || httpMethod.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST_INFO, "httpMethod");
        }
        if (clientIp == null || clientIp.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST_INFO, "clientIp");
        }
    }
}

