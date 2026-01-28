package org.example.sharedprompts.domain.admin.maintenance.service;

import java.time.Duration;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.admin.maintenance.config.AdminMaintenanceProperties;
import org.example.sharedprompts.domain.admin.maintenance.metrics.RebuildMetrics;
import org.example.sharedprompts.domain.admin.maintenance.rebuild.global.CommentRebuildExecutor;
import org.example.sharedprompts.domain.admin.maintenance.rebuild.global.PromptRebuildExecutor;
import org.example.sharedprompts.domain.admin.maintenance.rebuild.RebuildNotificationService;
import org.example.sharedprompts.domain.admin.maintenance.rebuild.RetryableRebuildService;
import org.example.sharedprompts.domain.admin.maintenance.rebuild.util.LocalRebuildStatusManager;
import org.example.sharedprompts.dto.admin.response.RebuildLikeCountsStatusResponseDto;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * 관리자 유지보수 서비스 구현체
 * 재빌드 작업을 오케스트레이션하는 역할만 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMaintenanceServiceImpl implements AdminMaintenanceService {

    private final AdminMaintenanceProperties properties;
    private final RebuildStatusService rebuildStatusService;
    private final RetryableRebuildService retryableRebuildService;
    private final RebuildNotificationService notificationService;
    private final LocalRebuildStatusManager localStatusManager;
    private final PromptRebuildExecutor promptRebuildExecutor;
    private final CommentRebuildExecutor commentRebuildExecutor;
    private final RebuildMetrics rebuildMetrics;
    private final MeterRegistry meterRegistry;

    @Override
    @Transactional(readOnly = true)
    public void rebuildLikeCountsFromDb() {
        long processedPrompts = promptRebuildExecutor.execute();
        long processedComments = commentRebuildExecutor.execute();
        
        log.info("좋아요 카운트 재빌드 완료 - 프롬프트: {}, 댓글: {}", processedPrompts, processedComments);
    }

    @Override
    @Async
    @SchedulerLock(
            name = "AdminMaintenanceService_rebuildLikeCounts",
            lockAtMostFor = "1h",
            lockAtLeastFor = "5m"
    )
    @LockProviderToUse("fallbackLockProvider")
    public void rebuildLikeCountsFromDbAsync() {
        // 주의: @SchedulerLock의 값은 컴파일 타임에 평가되므로 동적 변경이 불가능합니다.
        // Properties의 값은 다른 용도(로깅, 검증 등)로 사용하고,
        // 실제 Lock 설정은 어노테이션의 기본값을 사용합니다.
        rebuildMetrics.init();
        
        // 글로벌 상태 관리 사용 시 Redis에서 상태 확인
        boolean useGlobalStatus = properties.getRebuild().isUseGlobalStatus();
        if (useGlobalStatus && rebuildStatusService.isRunning()) {
            log.warn("좋아요 카운트 재빌드 작업이 이미 실행 중입니다. (글로벌 상태 체크)");
            rebuildMetrics.getShedLockFailureCounter().increment();
            notificationService.notifyShedLockFailure();
            return;
        }
        
        // 로컬 상태 체크 (추가 안전장치)
        if (!localStatusManager.acquireLockAndUpdateStatus()) {
            log.warn("좋아요 카운트 재빌드 작업이 이미 실행 중입니다. (로컬 상태 체크)");
            rebuildMetrics.getShedLockFailureCounter().increment();
            notificationService.notifyShedLockFailure();
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        LocalDateTime startTime = LocalDateTime.now();
        
        // 알림: 작업 시작
        notificationService.notifyStart();
        
        RebuildResult result;
        
        try {
            if (useGlobalStatus) {
                rebuildStatusService.saveStatus(
                        localStatusManager.getStatus(),
                        localStatusManager.getStartedAt(),
                        null,
                        null
                );
            }
            log.info("좋아요 카운트 재빌드 작업 시작");
            
            // 재시도 메커니즘 적용
            boolean retryEnabled = properties.getRebuild().getRetry().isEnabled();
            if (retryEnabled) {
                int maxRetries = properties.getRebuild().getRetry().getMaxRetries();
                long retryDelayMs = properties.getRebuild().getRetry().getRetryDelayMs();
                
                result = retryableRebuildService.executeWithRetry(
                    () -> {
                        long prompts = promptRebuildExecutor.execute();
                        long comments = commentRebuildExecutor.execute();
                        return new RebuildResult(prompts, comments);
                    },
                    maxRetries,
                    retryDelayMs
                );
            } else {
                long prompts = promptRebuildExecutor.execute();
                long comments = commentRebuildExecutor.execute();
                result = new RebuildResult(prompts, comments);
            }
            
            localStatusManager.updateStatusToCompleted();
            
            Duration duration = Duration.between(startTime, LocalDateTime.now());
            sample.stop(rebuildMetrics.getDurationTimer());
            rebuildMetrics.getSuccessCounter().increment();
            
            log.info("좋아요 카운트 재빌드 작업 완료 - 소요 시간: {}초, 처리된 프롬프트: {}, 처리된 댓글: {}", 
                    duration.getSeconds(), result.processedPrompts, result.processedComments);
            
            // 알림: 작업 완료
            notificationService.notifyCompleted(duration, result.processedPrompts, result.processedComments);
            
        } catch (Exception e) {
            Duration duration = Duration.between(startTime, LocalDateTime.now());
            sample.stop(rebuildMetrics.getDurationTimer());
            rebuildMetrics.getFailureCounter().increment();

            log.error("좋아요 카운트 재빌드 작업 중 오류 발생 - 소요 시간: {}초", duration.getSeconds(), e);
            localStatusManager.updateStatusToFailed(e);
            
            // 알림: 작업 실패
            notificationService.notifyFailed(e.getMessage(), duration);
            
            // 예외를 다시 던져서 ShedLock이 실패를 인지할 수 있도록 함
            throw e;
        } finally {
            localStatusManager.updateFinishedAt(LocalDateTime.now());
            // 글로벌 상태 관리 사용 시 Redis에 상태 저장
            if (useGlobalStatus) {
                try {
                    rebuildStatusService.saveStatus(
                            localStatusManager.getStatus(),
                            localStatusManager.getStartedAt(),
                            localStatusManager.getFinishedAt(),
                            localStatusManager.getErrorMessage()
                    );
                }catch (Exception ex) {
                    log.error("글로벌 상태 저장 실패", ex);
                }

            }
        }
    }
    
    /**
     * 재빌드 결과를 담는 내부 클래스
     */
    private static class RebuildResult {
        final long processedPrompts;
        final long processedComments;
        
        RebuildResult(long processedPrompts, long processedComments) {
            this.processedPrompts = processedPrompts;
            this.processedComments = processedComments;
        }
    }

    @Override
    public RebuildLikeCountsStatusResponseDto getRebuildLikeCountsStatus() {
        // 글로벌 상태 관리 사용 시 Redis에서 조회
        boolean useGlobalStatus = properties.getRebuild().isUseGlobalStatus();
        if (useGlobalStatus) {
            return rebuildStatusService.getStatus();
        }
        
        // 로컬 상태 반환
        return RebuildLikeCountsStatusResponseDto.builder()
                .status(localStatusManager.getStatus())
                .startedAt(localStatusManager.getStartedAt())
                .finishedAt(localStatusManager.getFinishedAt())
                .errorMessage(localStatusManager.getErrorMessage())
                .build();
    }
}

