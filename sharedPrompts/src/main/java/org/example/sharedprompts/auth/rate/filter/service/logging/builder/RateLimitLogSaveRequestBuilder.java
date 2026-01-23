package org.example.sharedprompts.auth.rate.filter.service.logging.builder;

import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.example.sharedprompts.dto.admin.request.RateLimitLogSaveRequest;
import org.example.sharedprompts.dto.admin.request.RateLimitRequestInfo;

import static org.example.sharedprompts.auth.rate.policy.RateLimitConstants.Response;

/**
 * Rate Limit 로그 저장 요청 DTO 빌더
 * 
 * RateLimitLogSaveRequest 생성을 담당합니다.
 * RateLimitLoggingService의 책임을 분리하여 단일 책임 원칙을 준수합니다.
 */
public final class RateLimitLogSaveRequestBuilder {

    private RateLimitLogSaveRequestBuilder() {
        // 유틸리티 클래스
    }

    /**
     * Rate Limit 로그 저장 요청 DTO를 생성합니다.
     * 
     * @param rule RateLimitRule
     * @param key Rate Limit 키 (타입 정보 포함)
     * @param result RateLimitResult
     * @param requestInfo HTTP 요청 정보
     * @param userId 사용자 ID (IP 기반인 경우 null)
     * @return RateLimitLogSaveRequest
     */
    public static RateLimitLogSaveRequest build(
            RateLimitRule rule,
            RateLimitKey key,
            RateLimiter.RateLimitResult result,
            RateLimitRequestInfo requestInfo,
            Long userId
    ) {
        long retryAfter = result.getRetryAfter(Response.MIN_RETRY_AFTER_SECONDS);
        
        return RateLimitLogSaveRequest.builder()
                .ruleName(rule.getName())
                .keyValue(key.value())
                .currentCount(result.currentCount())
                .capacity(rule.getCapacity())
                .userId(userId)
                .clientIp(requestInfo.clientIp())
                .uri(requestInfo.uri())
                .httpMethod(requestInfo.httpMethod())
                .retryAfter(retryAfter)
                .rateLimitType(key.type())
                .build();
    }
}

