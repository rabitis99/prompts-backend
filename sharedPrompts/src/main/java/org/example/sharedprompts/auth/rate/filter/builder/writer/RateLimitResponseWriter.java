package org.example.sharedprompts.auth.rate.filter.builder.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.filter.util.RateLimitHeaderUtil;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Rate Limit 초과 시 응답 작성 유틸리티
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RateLimitResponseWriter {

    private static final String CHARACTER_ENCODING = "UTF-8";

    /**
     * Rate Limit 초과 응답을 작성합니다.
     * 
     * @param response HttpServletResponse
     * @param objectMapper ObjectMapper
     * @param rule RateLimitRule
     * @param result RateLimitResult
     * @param errorCode 사용할 에러 코드 (null이면 기본값 RATE_LIMIT_EXCEEDED 사용)
     * @param redisTemplate RedisTemplate (사용하지 않음, 하위 호환성을 위해 유지)
     * @param rateLimitKey Redis 키 (사용하지 않음, 하위 호환성을 위해 유지)
     * @throws IOException 응답 작성 실패 시
     */
    public static void writeTooManyRequests(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            RateLimitRule rule,
            RateLimiter.RateLimitResult result,
            ErrorCode errorCode,
            @Nullable RedisTemplate<String, Object> redisTemplate,
            @Nullable String rateLimitKey
    ) throws IOException {
        ErrorCode code = (errorCode != null) ? errorCode : ErrorCode.RATE_LIMIT_EXCEEDED;
        
        // null 안전성 검사 (방어적 프로그래밍)
        if (result == null) {
            throw new IllegalArgumentException("RateLimitResult cannot be null");
        }
        
        // RateLimit details 생성 (요구사항: details 필드)
        // 헤더와 동일한 reset timestamp를 사용하여 일관성 유지
        long limit = rule.getCapacity();
        long remaining = Math.max(0, limit - result.currentCount());
        long retryAfter = result.getRetryAfter(1L);
        
        // 헤더와 동일한 방식으로 reset timestamp 계산 (정확성 보장)
        long resetTimestamp = calculateResetTimestamp(rule, result);
        
        java.util.Map<String, Object> details = java.util.Map.of(
                "limit", limit,
                "remaining", remaining,
                "reset", resetTimestamp,
                "retryAfter", retryAfter
        );
        
        // ExceptionDto에 details 포함
        org.example.sharedprompts.global.exception.dto.ExceptionDto errorDto = 
                org.example.sharedprompts.global.exception.dto.ExceptionDto.of(code, null, details);
        
        CustomResponse<Void> body = new CustomResponse<>(
                code.getHttpStatus(),
                false,
                null,
                errorDto
        );

        // HTTP 상태 코드 설정
        response.setStatus(code.getHttpStatus().value());
        
        // RateLimit 헤더 추가 (요구사항: X-RateLimit-*, Retry-After)
        RateLimitHeaderUtil.addRateLimitExceededHeaders(
                response, rule, result, redisTemplate, rateLimitKey
        );
        
        // Content-Type 및 인코딩 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        // 응답 본문 작성
        objectMapper.writeValue(response.getWriter(), body);
        
        // 응답 버퍼 플러시하여 응답 커밋 (필터 체인 중단을 위해 필수)
        response.flushBuffer();
    }

    /**
     * 리셋 타임스탬프를 계산합니다.
     * 
     * 헤더와 details 필드에서 동일한 reset timestamp를 사용하여 일관성을 보장합니다.
     * RateLimitHeaderUtil의 calculateResetTimestamp와 동일한 로직을 사용합니다.
     */
    private static long calculateResetTimestamp(
            RateLimitRule rule,
            RateLimiter.RateLimitResult result
    ) {
        long now = java.time.Instant.now().getEpochSecond();
        
        // 1. result가 있는 경우: ttlSeconds 또는 retryAfterSeconds 사용
        if (result != null) {
            // 초과된 요청: retryAfterSeconds 사용 (이미 TTL 기반으로 계산됨)
            if (result.retryAfterSeconds() > 0) {
                return now + result.retryAfterSeconds();
            }
            // 성공한 요청: ttlSeconds 사용
            if (result.ttlSeconds() > 0) {
                return now + result.ttlSeconds();
            }
        }
        
        // 2. 폴백: windowSeconds 사용 (result가 null이거나 TTL이 없는 경우)
        return now + rule.getWindowSeconds();
    }
}


