package org.example.sharedprompts.module.domain.production.service.job.state;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Component;

@Component
public class JobStateMachine {

    public void start(JobEntity job) {
        if (job.getStatus() != JobStatus.PENDING) {
            throw new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    String.format("Cannot start job: expected PENDING, but was %s", job.getStatus())
            );
        }
        job.start();
    }

    public void complete(JobEntity job, String artifactId) {
        if (job.getStatus() != JobStatus.PROCESSING) {
            throw new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    String.format("Cannot complete job: expected PROCESSING, but was %s", job.getStatus())
            );
        }
        job.complete(artifactId);
    }

    public void fail(JobEntity job, String errorMessage) {
        if (job.getStatus() != JobStatus.PROCESSING) {
            throw new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    String.format("Cannot fail job: expected PROCESSING, but was %s", job.getStatus())
            );
        }
        job.fail(errorMessage);
    }

    public void retry(JobEntity job) {
        if (job.getStatus() != JobStatus.FAILED) {
            throw new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    String.format("Cannot retry job: expected FAILED, but was %s", job.getStatus())
            );
        }
        job.retry();
    }

    /**
     * P2-2: FAILED → RETRYING (메시지 레벨 retry 발행 성공 시)
     */
    public void markAsRetrying(JobEntity job) {
        if (job.getStatus() != JobStatus.FAILED) {
            throw new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    String.format("Cannot mark as retrying: expected FAILED, but was %s", job.getStatus())
            );
        }
        job.markAsRetrying();
    }

    /**
     * P2-2: RETRYING → PROCESSING (consumer가 retry 메시지 소비 시)
     */
    public void startFromRetrying(JobEntity job) {
        if (job.getStatus() != JobStatus.RETRYING) {
            throw new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    String.format("Cannot start from retrying: expected RETRYING, but was %s", job.getStatus())
            );
        }
        job.startFromRetrying();
    }

    /**
     * P3-1: PROCESSING → UNKNOWN (AI/S3 timeout 발생 시)
     */
    public void markAsUnknown(JobEntity job, String reason) {
        if (job.getStatus() != JobStatus.PROCESSING) {
            throw new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    String.format("Cannot mark as unknown: expected PROCESSING, but was %s", job.getStatus())
            );
        }
        job.markAsUnknown(reason);
    }

    /**
     * P3-1: UNKNOWN → SUCCEEDED (복구 스케줄러가 성공 확인 시)
     */
    public void recoverAsSucceeded(JobEntity job, String artifactId) {
        if (job.getStatus() != JobStatus.UNKNOWN) {
            throw new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    String.format("Cannot recover as succeeded: expected UNKNOWN, but was %s", job.getStatus())
            );
        }
        job.recoverAsSucceeded(artifactId);
    }

    /**
     * P3-1: UNKNOWN → FAILED (복구 스케줄러가 실패 확인 또는 최대 재조회 초과 시)
     */
    public void recoverAsFailed(JobEntity job, String errorMessage) {
        if (job.getStatus() != JobStatus.UNKNOWN) {
            throw new BaseException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    null,
                    String.format("Cannot recover as failed: expected UNKNOWN, but was %s", job.getStatus())
            );
        }
        job.recoverAsFailed(errorMessage);
    }
}
