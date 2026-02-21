package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.model.job.Job;

/**
 * JobResponseDto 매퍼
 */
public final class JobResponseDtoMapper {

    private JobResponseDtoMapper() {
    }

    /**
     * Job 모델을 JobResponseDto로 변환합니다.
     * 
     * @param job 변환할 Job 모델
     * @return 변환된 JobResponseDto
     */
    public static JobResponseDto toDto(Job job) {
        if (job == null) {
            return null;
        }
        return new JobResponseDto(
                job.getJobId(),
                job.getStatus(),
                job.getArtifactId(),
                job.getProductionId(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }
}

