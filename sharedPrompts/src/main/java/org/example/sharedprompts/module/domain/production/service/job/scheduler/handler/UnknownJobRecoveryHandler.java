package org.example.sharedprompts.module.domain.production.service.job.scheduler.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.springframework.stereotype.Component;

/**
 * P3-1: UNKNOWN 상태 Job 복구 핸들러
 *
 * UNKNOWN 상태는 AI/S3 호출 timeout으로 성공/실패 확정이 불가한 상태.
 * 즉시 재호출 금지 - AI provider 중복 과금 위험.
 *
 * 현재 전략: threshold 초과 시 FAILED로 전이 (운영자 수동 조사 권고)
 * 향후 개선: AI provider별 status 조회 API 연동으로 실제 성공/실패 판단
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnknownJobRecoveryHandler implements JobRecoveryHandler {

    private final JobStateService jobStateService;

    @Override
    public boolean supports(JobStatus status) {
        return status == JobStatus.UNKNOWN;
    }

    @Override
    public void recover(JobEntity job) {
        log.warn("Recovering UNKNOWN job - jobId: {}, retryCount: {}, errorMessage: {}",
                job.getJobId(), job.getRetryCount(), job.getErrorMessage());

        // P3-1: UNKNOWN → FAILED (최대 대기 시간 초과)
        // AI provider에 중복 요청을 보내지 않기 위해 즉시 재처리하지 않음.
        // 운영자가 AI provider 콘솔에서 실제 처리 결과를 확인해야 함.
        // TODO: AI provider별 status 조회 API 연동 (Leonardo: GET /generations/{id}, Groq: 조회 방법 확인)
        String failureReason = String.format(
                "UNKNOWN state timeout: job exceeded recovery threshold. " +
                "Check AI provider console for actual result. Original reason: %s",
                job.getErrorMessage()
        );

        jobStateService.recoverUnknownJobAsFailed(job.getJobId(), failureReason);

        log.warn("UNKNOWN job resolved as FAILED - jobId: {}. " +
                "Manual verification recommended: check AI provider for actual result.", job.getJobId());
    }
}
