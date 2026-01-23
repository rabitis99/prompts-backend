package org.example.sharedprompts.dto.admin.request;

import lombok.Builder;
import org.example.sharedprompts.domain.rate.ratelimitlog.enums.RateLimitType;

/**
 * Rate Limit 로그 저장 요청 DTO
 * 
 * 비동기 로그 저장 시 필요한 모든 정보를 담는 DTO입니다.
 * 파라미터 과다 문제를 해결하고 가독성을 향상시킵니다.
 */
@Builder
public record RateLimitLogSaveRequest(
        String ruleName,
        String keyValue,
        Long currentCount,
        Long capacity,
        Long userId,
        String clientIp,
        String uri,
        String httpMethod,
        long retryAfter,
        RateLimitType rateLimitType
) {
}





