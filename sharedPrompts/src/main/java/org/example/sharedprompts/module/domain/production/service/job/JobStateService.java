package org.example.sharedprompts.module.domain.production.service.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.example.sharedprompts.module.domain.production.service.job.helper.JobUpdateHelper;
import org.example.sharedprompts.module.domain.production.service.job.state.JobStateMachine;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.JobProcessingException;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.domain.production.service.production.ProductionArtifactService;
import org.example.sharedprompts.module.domain.production.util.ArtifactMetadataHelper;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobStateService {

    private final JobRepository jobRepository;
    private final JobUpdateHelper jobUpdateHelper;
    private final ProductionArtifactService productionArtifactService;
    private final JobStateMachine stateMachine;

    public void updateJobPromptVersion(String jobId, String promptVersion) {
        jobUpdateHelper.updateJob(jobId, job -> job.setPromptVersion(promptVersion));
    }

    public void setModelInfo(String jobId, String modelName, String tokenUsage) {
        jobUpdateHelper.updateJob(jobId, job -> job.setModelInfo(modelName, tokenUsage));
    }

    public void markStored(String jobId, String s3Key) {
        JobEntity job = getJob(jobId);

        String estimatedContentType = ArtifactMetadataHelper.determineContentType(s3Key);
        ProductionCommandType commandType;
        try {
            commandType = ProductionCommandType.valueOf(job.getCommandType());
        } catch (IllegalArgumentException e) {
            log.error("Invalid command type: {} - jobId: {}", job.getCommandType(), jobId, e);
            throw new JobProcessingException(
                    ModuleErrorCode.JOB_INVALID_STATUS,
                    "Invalid command type: " + job.getCommandType() + " for job: " + jobId
            );
        }
        
        if (commandType == ProductionCommandType.IMAGE 
                && estimatedContentType != null 
                && "text/plain".equals(estimatedContentType)) {
            log.info("Skipping prompt txt file artifact creation for IMAGE command - jobId: {}, s3Key: {}", jobId, s3Key);
            if (job.getArtifactId() != null && !job.getArtifactId().isBlank()) {
                log.info("Using existing artifactId for prompt txt file - jobId: {}, artifactId: {}", jobId, job.getArtifactId());
            } else {
                log.info("No artifactId for prompt txt file - jobId: {} (waiting for image artifact)", jobId);
            }
            return;
        }

        var artifact = productionArtifactService.createArtifact(job, s3Key);
        String artifactId = artifact.getId().toString();

        try {
            jobUpdateHelper.updateJob(jobId, jobEntity -> stateMachine.complete(jobEntity, artifactId));
            log.info("ProductionArtifact created and job completed - jobId: {}, artifactId: {}, s3Key: {}",
                    jobId, artifactId, s3Key);
        } catch (Exception e) {
            log.error("Failed to complete job after artifact creation - jobId: {}, artifactId: {}. Cleaning up orphaned artifact.",
                    jobId, artifactId, e);
            try {
                productionArtifactService.deleteArtifact(artifactId);
                log.info("Orphaned artifact deleted - jobId: {}, artifactId: {}", jobId, artifactId);
            } catch (Exception cleanupException) {
                log.error("Failed to cleanup orphaned artifact - jobId: {}, artifactId: {}", jobId, artifactId, cleanupException);
            }
            throw e;
        }
    }

    public void markCompleted(String jobId) {
        jobUpdateHelper.updateJob(jobId, jobEntity -> {
            if (jobEntity.getArtifactId() == null || jobEntity.getArtifactId().isBlank()) {
                throw new JobProcessingException(
                        ModuleErrorCode.JOB_INVALID_STATUS,
                        "Cannot complete job without artifactId - jobId: " + jobId
                );
            }
            stateMachine.complete(jobEntity, jobEntity.getArtifactId());
        });
    }

    public void saveJobFailure(String jobId, String errorMessage) {
        try {
            jobUpdateHelper.updateJob(jobId, job -> stateMachine.fail(job, errorMessage));
            log.info("Job failure saved - jobId: {}, error: {}", jobId, errorMessage);
        } catch (BaseException e) {
            if (e.getErrorCode() == ModuleErrorCode.JOB_INVALID_STATUS ||
                e.getErrorCode() == ModuleErrorCode.JOB_ALREADY_COMPLETED ||
                e.getErrorCode() == ModuleErrorCode.JOB_ALREADY_FAILED) {
                log.warn("Could not mark job as failed (already in final state) - jobId: {}", jobId);
            } else {
                throw e;
            }
        } catch (IllegalStateException e) {
            log.warn("Could not mark job as failed (already in final state) - jobId: {}, error: {}", jobId, e.getMessage());
        }
    }

    /**
     * P3-1: AI/S3 timeout 발생 시 UNKNOWN 상태로 전이
     * Timeout은 성공/실패 확정 불가 → 즉시 재호출 금지
     * UnknownJobRecoveryScheduler가 5분 주기로 AI provider 상태 조회 후 복구
     */
    public void saveJobUnknown(String jobId, String reason) {
        try {
            jobUpdateHelper.updateJob(jobId, job -> stateMachine.markAsUnknown(job, reason));
            log.warn("Job marked as UNKNOWN (timeout ambiguity) - jobId: {}, reason: {}", jobId, reason);
        } catch (BaseException e) {
            if (e.getErrorCode() == ModuleErrorCode.JOB_INVALID_STATUS) {
                log.warn("Could not mark job as UNKNOWN (invalid state transition) - jobId: {}", jobId);
            } else {
                throw e;
            }
        } catch (IllegalStateException e) {
            log.warn("Could not mark job as UNKNOWN - jobId: {}, error: {}", jobId, e.getMessage());
        }
    }

    /**
     * P2-2: 메시지 레벨 retry 발행 성공 시 RETRYING 상태로 전이
     * 운영 대시보드에서 retry 대기 중인 job을 명시적으로 식별 가능
     */
    public void markJobAsRetrying(String jobId) {
        try {
            jobUpdateHelper.updateJob(jobId, job -> stateMachine.markAsRetrying(job));
            log.info("Job marked as RETRYING - jobId: {}", jobId);
        } catch (BaseException e) {
            if (e.getErrorCode() == ModuleErrorCode.JOB_INVALID_STATUS) {
                log.warn("Could not mark job as RETRYING (invalid state) - jobId: {}", jobId);
            } else {
                throw e;
            }
        } catch (IllegalStateException e) {
            log.warn("Could not mark job as RETRYING - jobId: {}, error: {}", jobId, e.getMessage());
        }
    }

    /**
     * P3-1: UNKNOWN → FAILED (복구 스케줄러가 threshold 초과 판단 시)
     */
    public void recoverUnknownJobAsFailed(String jobId, String reason) {
        try {
            jobUpdateHelper.updateJob(jobId, job -> stateMachine.recoverAsFailed(job, reason));
            log.warn("UNKNOWN job recovered as FAILED - jobId: {}", jobId);
        } catch (BaseException e) {
            if (e.getErrorCode() == ModuleErrorCode.JOB_INVALID_STATUS) {
                log.warn("Could not recover UNKNOWN job as FAILED (invalid state) - jobId: {}", jobId);
            } else {
                throw e;
            }
        } catch (IllegalStateException e) {
            log.warn("Could not recover UNKNOWN job as FAILED - jobId: {}, error: {}", jobId, e.getMessage());
        }
    }

    /**
     * FAILED job을 PENDING으로 전이 후 재처리 가능하게 합니다.
     * JOB_INVALID_STATUS 등은 경고 로그만 남기고, 그 외 예외는 호출자에게 전파합니다.
     */
    public void retryJob(String jobId) {
        try {
            jobUpdateHelper.updateJob(jobId, job -> stateMachine.retry(job));
            log.info("Job retried - jobId: {}", jobId);
        } catch (BaseException e) {
            if (e.getErrorCode() == ModuleErrorCode.JOB_INVALID_STATUS) {
                log.warn("Could not retry job (invalid state) - jobId: {}", jobId);
            } else {
                throw e;
            }
        } catch (IllegalStateException e) {
            log.warn("Could not retry job - jobId: {}, error: {}", jobId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public JobEntity getJob(String jobId) {
        return jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new JobProcessingException(ModuleErrorCode.JOB_NOT_FOUND, "Job not found: " + jobId));
    }
}
