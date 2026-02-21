package org.example.sharedprompts.module.domain.production.service.job.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job 복구 스케줄러
 * 단일 책임: 스케줄링 + 락 관리 + Service 호출만 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobRecoveryScheduler {

    private final SchedulerJobRecoveryService schedulerJobRecoveryService;

    /**
     * 만료된 Job 복구 스케줄러 (production_jobs 조회 간격)
     */
    @Scheduled(fixedDelayString = "${production.job.recovery.interval-ms:120000}")
    @SchedulerLock(
            name = "JobRecoveryScheduler",
            lockAtMostFor = "5m",
            lockAtLeastFor = "30s"
    )
    @LockProviderToUse("fallbackLockProvider")
    @Transactional
    public void recoverStaleJobs() {
        schedulerJobRecoveryService.recoverStaleJobs();
    }
}

