package org.example.sharedprompts.auth.rate.filter.builder.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.filter.util.RateLimitHeaderUtil;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.jetbrains.annotations.Nullable;
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
     * @param redisTemplate RedisTemplate (TTL 조회용, null 가능)
     * @param rateLimitKey Redis 키 (TTL 조회용, null 가능)
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
        long resetTimestamp = calculateResetTimestamp(rule, result, redisTemplate, rateLimitKey);
        
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
    }

    /**
     * 리셋 타임스탬프를 계산합니다.
     * 
     * 헤더와 details 필드에서 동일한 reset timestamp를 사용하여 일관성을 보장합니다.
     * RateLimitHeaderUtil의 calculateResetTimestamp와 동일한 로직을 사용합니다.
     */
    private static long calculateResetTimestamp(
            RateLimitRule rule,
            RateLimiter.RateLimitResult result,
            @Nullable RedisTemplate<String, Object> redisTemplate,
            @Nullable String rateLimitKey
    ) {
        long now = java.time.Instant.now().getEpochSecond();
        
        // 1. 초과된 요청: retryAfterSeconds를 사용 (이미 TTL 기반으로 계산됨)
        if (result != null && result.retryAfterSeconds() > 0) {
            return now + result.retryAfterSeconds();
        }
        
        // 2. 성공한 요청: Redis TTL을 직접 조회하여 정확한 reset 시간 계산
        if (redisTemplate != null && rateLimitKey != null) {
            try {
                Long ttl = redisTemplate.getExpire(rateLimitKey, java.util.concurrent.TimeUnit.SECONDS);
                if (ttl != null && ttl > 0) {
                    return now + ttl;
                }
            } catch (Exception e) {
                // TTL 조회 실패 시 폴백 사용 (예외를 무시하고 폴백으로 진행)
            }
        }
        
        // 3. 폴백: windowSeconds 사용 (정확하지 않을 수 있음)
        return now + rule.getWindowSeconds();
    }
}


