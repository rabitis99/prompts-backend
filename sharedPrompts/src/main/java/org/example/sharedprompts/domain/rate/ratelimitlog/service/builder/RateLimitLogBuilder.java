package org.example.sharedprompts.domain.rate.ratelimitlog.service.builder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.dto.admin.request.RateLimitLogSaveRequest;
import org.example.sharedprompts.domain.rate.ratelimitlog.RateLimitLog;
import org.example.sharedprompts.domain.user.User;

/**
 * Rate Limit 로그 빌더
 * 
 * RateLimitLog 엔티티 생성을 담당합니다.
 * DTO 기반으로 파라미터 과다 문제를 해결했습니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RateLimitLogBuilder {

    /**
     * Rate Limit 로그를 생성합니다.
     * 
     * @param saveRequest 로그 저장 요청 DTO
     * @param user User 엔티티 (null 가능)
     * @return RateLimitLog
     */
    public static RateLimitLog build(RateLimitLogSaveRequest saveRequest, User user) {
        return RateLimitLog.builder()
                .ruleName(saveRequest.ruleName())
                .rateLimitKey(saveRequest.keyValue())
                .currentCount(saveRequest.currentCount())
                .capacity(saveRequest.capacity())
                .retryAfter(saveRequest.retryAfter())
                .user(user)
                .clientIp(saveRequest.clientIp())
                .uri(saveRequest.uri())
                .httpMethod(saveRequest.httpMethod())
                .rateLimitType(saveRequest.rateLimitType())
                .build();
    }
}


