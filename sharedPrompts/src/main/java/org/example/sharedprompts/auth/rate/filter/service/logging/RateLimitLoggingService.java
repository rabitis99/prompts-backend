package org.example.sharedprompts.auth.rate.filter.service.logging;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.filter.model.RateLimitKey;
import org.example.sharedprompts.auth.rate.filter.service.logging.builder.RateLimitLogSaveRequestBuilder;
import org.example.sharedprompts.auth.rate.filter.service.logging.extractor.RateLimitRequestInfoExtractor;
import org.example.sharedprompts.dto.admin.request.RateLimitLogSaveRequest;
import org.example.sharedprompts.dto.admin.request.RateLimitRequestInfo;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;
import org.example.sharedprompts.domain.rate.ratelimitlog.RateLimitLog;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.builder.RateLimitLogBuilder;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.exception.RateLimitLogExceptionHandler;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.file.RateLimitFileLogService;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.user.RateLimitLogUserService;
import org.example.sharedprompts.domain.rate.ratelimitlog.repository.RateLimitLogRepository;
import org.example.sharedprompts.domain.user.User;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rate Limit 로깅 서비스
 * 
 * Rate Limit 관련 로그를 기록하는 서비스입니다.
 * 파일 로그와 데이터베이스 저장을 모두 수행합니다.
 * DB 저장은 비동기로 처리되어 요청 응답 시간에 영향을 주지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class RateLimitLoggingService {
    
    private final RateLimitFileLogService fileLogService;
    private final RateLimitLogRepository rateLimitLogRepository;
    private final RateLimitLogUserService userService;
    private final RateLimitLogExceptionHandler exceptionHandler;

    /**
     * Rate Limit 초과 로그를 기록합니다.
     * 파일 로그는 동기적으로 기록하고, 데이터베이스 저장은 비동기로 처리합니다.
     * 
     * @param rule RateLimitRule
     * @param key Rate Limit 키 (타입 정보 포함)
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
                rule, key, result, requestInfo, userId
        );
        
        // 데이터베이스 저장 (비동기)
        saveRateLimitLogAsync(saveRequest);
    }

    /**
     * Rate Limit 로그를 데이터베이스에 비동기로 저장합니다.
     * 별도 트랜잭션으로 처리되어 메인 트랜잭션에 영향을 주지 않습니다.
     * 
     * @param saveRequest 로그 저장 요청 DTO
     */
    @Async("rateLimitLogTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRateLimitLogAsync(RateLimitLogSaveRequest saveRequest) {
        try {
            // User 조회
            User user = userService.findUserSafely(saveRequest.userId());

            // RateLimitLog 엔티티 생성 (DTO 기반)
            RateLimitLog log = RateLimitLogBuilder.build(saveRequest, user);

            // 저장
            rateLimitLogRepository.save(log);
        } catch (Exception e) {
            exceptionHandler.handleException(saveRequest.ruleName(), saveRequest.keyValue(), e);
        }
    }

    /**
     * Rate Limit 체크 실패 로그를 기록합니다.
     * 
     * @param key Rate Limit 키
     * @param error 예외 메시지
     * @param exception 예외 객체
     */
    public void logRateLimitCheckFailed(String key, String error, Exception exception) {
        fileLogService.logRateLimitCheckFailed(key, error, exception);
    }

    /**
     * 잘못된 HTTP Method 로그를 기록합니다.
     * 
     * @param method HTTP Method 문자열
     */
    public void logInvalidHttpMethod(String method) {
        fileLogService.logInvalidHttpMethod(method);
    }
}

