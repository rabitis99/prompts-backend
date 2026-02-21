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
     * productionId가 null이면 artifactId가 숫자일 경우 그 값으로 채웁니다(마이그레이션 백필 전 기존 행 대응).
     *
     * @param job 변환할 Job 모델
     * @return 변환된 JobResponseDto
     */
    public static JobResponseDto toDto(Job job) {
        if (job == null) {
            return null;
        }
        Long productionId = job.getProductionId();
        if (productionId == null && job.getArtifactId() != null) {
            productionId = parseProductionIdOrNull(job.getArtifactId());
        }
        return new JobResponseDto(
                job.getJobId(),
                job.getStatus(),
                job.getArtifactId(),
                productionId,
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }

    private static Long parseProductionIdOrNull(String artifactId) {
        if (artifactId == null || artifactId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(artifactId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

