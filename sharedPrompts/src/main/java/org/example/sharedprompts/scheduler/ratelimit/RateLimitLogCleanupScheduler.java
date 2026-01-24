package org.example.sharedprompts.scheduler.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.rate.ratelimitlog.repository.RateLimitLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Rate Limit 로그 정리 스케줄러
 * 
 * 오래된 Rate Limit 로그를 주기적으로 삭제하여 DB 용량을 관리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitLogCleanupScheduler {

    private final RateLimitLogRepository rateLimitLogRepository;
    
    /**
     * Clock을 주입받아 타임존을 명시적으로 제어합니다.
     * UTC를 사용하여 저장된 createdAt과 일치하도록 보장합니다.
     */
    private final Clock clock;

    @Value("${rate-limit.log.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${rate-limit.log.cleanup.retention-days:90}")
    private int retentionDays;

    /**
     * 오래된 Rate Limit 로그 삭제
     * - 매일 새벽 3시에 실행
     * - 기본적으로 90일 이상 된 로그 삭제
     */
    @Scheduled(cron = "${rate-limit.log.cleanup.schedule:0 0 3 * * ?}")
    @SchedulerLock(
            name = "RateLimitLogCleanupScheduler",
            lockAtMostFor = "30m",
            lockAtLeastFor = "5m"
    )
    @Transactional
    public void cleanupOldLogs() {
        if (!cleanupEnabled) {
            log.debug("Rate Limit log cleanup is disabled");
            return;
        }

        if (retentionDays <= 0) {
            log.warn("Rate Limit log cleanup skipped due to invalid retentionDays={}", retentionDays);
            return;
        }

        log.info("RateLimitLogCleanupScheduler started: retentionDays={}", retentionDays);

        // UTC 기준으로 현재 시간을 가져와 저장된 createdAt과 타임존을 일치시킵니다.
        // Clock을 통해 명시적으로 타임존을 제어하여 클라우드 환경에서도 일관된 동작을 보장합니다.
        LocalDateTime cutoffDate = LocalDateTime.now(clock).minusDays(retentionDays);
        try {
            int deletedCount = rateLimitLogRepository.deleteOldLogs(cutoffDate);
            log.info("RateLimitLogCleanupScheduler finished: deleted {} old rate limit logs", deletedCount);
        } catch (Exception e) {
            log.error("RateLimitLogCleanupScheduler failed: cutoffDate={}", cutoffDate, e);
            throw e;
        }
    }
}


