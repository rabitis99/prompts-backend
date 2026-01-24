package org.example.sharedprompts.auth.rate.filter.service.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.dto.admin.request.RateLimitLogSaveRequest;
import org.example.sharedprompts.domain.rate.ratelimitlog.RateLimitLog;
import org.example.sharedprompts.domain.rate.ratelimitlog.repository.RateLimitLogRepository;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.builder.RateLimitLogBuilder;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.exception.RateLimitLogExceptionHandler;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.user.RateLimitLogUserService;
import org.example.sharedprompts.domain.user.User;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rate Limit 로그 비동기 저장 서비스
 * 
 * Rate Limit 로그를 데이터베이스에 비동기로 저장하는 전용 서비스입니다.
 * 별도 빈으로 분리하여 Spring 프록시를 통해 @Async와 @Transactional이 정상 작동하도록 합니다.
 * 
 * <p>주의: 이 서비스는 별도 빈으로 분리되어 있어 RateLimitLoggingService에서
 * 의존성 주입을 통해 호출하면 Spring 프록시를 거치므로 @Async와 @Transactional이 정상 작동합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitLogAsyncService {

    private final RateLimitLogRepository rateLimitLogRepository;
    private final RateLimitLogUserService userService;
    private final RateLimitLogExceptionHandler exceptionHandler;

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
}

