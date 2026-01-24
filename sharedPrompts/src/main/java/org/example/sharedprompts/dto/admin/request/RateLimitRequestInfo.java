package org.example.sharedprompts.dto.admin.request;

/**
 * Rate Limit 로그를 위한 HTTP 요청 정보 DTO
 * 
 * HTTP 요청에서 추출한 정보를 담는 불변 레코드입니다.
 * DTO는 단순 운반체로 유지하며, 검증은 RateLimitRequestInfoExtractor에서 수행합니다.
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
    // 검증 로직은 RateLimitRequestInfoExtractor로 이동
    // fail-open 정책을 고려하여 예외 대신 기본값 사용
}

