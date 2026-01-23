package org.example.sharedprompts.auth.rate.filter.service.logging;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.filter.service.logging.builder.RateLimitLogSaveRequestBuilder;
import org.example.sharedprompts.auth.rate.filter.service.logging.extractor.RateLimitRequestInfoExtractor;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.file.RateLimitFileLogService;
import org.example.sharedprompts.dto.admin.request.RateLimitLogSaveRequest;
import org.example.sharedprompts.dto.admin.request.RateLimitRequestInfo;
import org.springframework.stereotype.Service;

/**
 * Rate Limit 로그 서비스
 * 
 * Rate Limit 초과 및 체크 실패 로그를 기록합니다.
 * 파일 로그와 데이터베이스 로그를 모두 기록합니다.
 */
@Service
@RequiredArgsConstructor
public class RateLimitLoggingService {

    private final RateLimitFileLogService fileLogService;
    private final RateLimitLogAsyncService asyncService;

    /**
     * Rate Limit 초과 로그를 기록합니다.
     * 
     * @param rule RateLimitRule
     * @param key Rate Limit 키
     * @param result RateLimitResult
     * @param request HttpServletRequest
     * @param userId 사용자 ID (IP 기반인 경우 null)
     */
    public void logRateLimitExceeded(
            RateLimitRule rule,
            RateLimitKey key,
            RateLimiter.RateLimitResult result,
            HttpServletRequest request,
            Long userId
    ) {
        // HTTP 요청 정보 추출 (책임 분리)
        RateLimitRequestInfo requestInfo = RateLimitRequestInfoExtractor.extract(request);
        
        // 파일 로그 기록 (동기) - key.value()를 사용하여 기존 String 기반 로깅과 호환
        fileLogService.logRateLimitExceeded(rule, key.value(), result, request, userId);
        
        // 데이터베이스 저장 요청 DTO 생성 (책임 분리)
        RateLimitLogSaveRequest saveRequest = RateLimitLogSaveRequestBuilder.build(
                rule,
                key,
                result,
                requestInfo,
                userId
        );
        
        // 데이터베이스 저장 (비동기) - 별도 빈으로 분리하여 Spring 프록시가 정상 작동하도록 함
        asyncService.saveRateLimitLogAsync(saveRequest);
    }

    /**
     * Rate Limit 체크 실패 로그를 기록합니다.
     * 
     * @param key Rate Limit 키
     * @param error 에러 메시지
     * @param exception 발생한 예외
     */
    public void logRateLimitCheckFailed(String key, String error, Exception exception) {
        fileLogService.logRateLimitCheckFailed(key, error, exception);
    }
}
