package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.model.production.ProductionStatus;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandlerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductionResponseDtoMapper 테스트")
class ProductionResponseDtoTest {

    @Mock
    private ArtifactHandlerRegistry artifactHandlerRegistry;

    private ProductionArtifactEntity artifactWithPrimaryTextDetail() {
        ProductionArtifactEntity entity = ProductionArtifactEntity.builder()
                .jobId(1L)
                .tenantId("tenant-1")
                .userId(100L)
                .commandType(ProductionCommandType.TEXT)
                .build();
        ProductionArtifactDetailEntity detail = ProductionArtifactDetailEntity.builder()
                .artifactType(ArtifactType.TEXT)
                .content("생성된 텍스트")
                .fileName("output.txt")
                .contentType("text/plain")
                .build();
        entity.addArtifact(detail);
        entity.markAsPrimary(detail);
        return entity;
    }

    private ProductionArtifactEntity emptyArtifact() {
        return ProductionArtifactEntity.builder()
                .jobId(1L)
                .tenantId("tenant-1")
                .userId(100L)
                .commandType(ProductionCommandType.TEXT)
                .build();
    }

    private JobEntity succeededJob() {
        JobEntity job = JobEntity.builder()
                .jobId("job-1").idempotencyKey("k1").promptId(1L)
                .userId(100L).tenantId("tenant-1").commandType("TEXT").commandJson("{}")
                .build();
        job.start();
        job.complete("artifact-1");
        return job;
    }

    private JobEntity failedJob() {
        JobEntity job = JobEntity.builder()
                .jobId("job-2").idempotencyKey("k2").promptId(1L)
                .userId(100L).tenantId("tenant-1").commandType("TEXT").commandJson("{}")
                .build();
        job.start();
        job.fail("오류 발생");
        return job;
    }

    private JobEntity processingJob() {
        JobEntity job = JobEntity.builder()
                .jobId("job-3").idempotencyKey("k3").promptId(1L)
                .userId(100L).tenantId("tenant-1").commandType("TEXT").commandJson("{}")
                .build();
        job.start();
        return job;
    }

    // ===== Job 정보 있는 경우 =====

    @Test
    @DisplayName("Job이 SUCCEEDED이면 status=SUCCEEDED, startedAt/completedAt 포함")
    void toDto_with_succeeded_job_returns_succeeded_status() {
        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(
                artifactWithPrimaryTextDetail(), succeededJob(), artifactHandlerRegistry);

        assertThat(dto.status()).isEqualTo(ProductionStatus.SUCCEEDED);
        assertThat(dto.errorMessage()).isNull();
        assertThat(dto.startedAt()).isNotNull();
        assertThat(dto.completedAt()).isNotNull();
    }

    @Test
    @DisplayName("Job이 FAILED이면 status=FAILED, errorMessage 포함, artifact 없음")
    void toDto_with_failed_job_returns_failed_status_with_error_message() {
        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(
                emptyArtifact(), failedJob(), artifactHandlerRegistry);

        assertThat(dto.status()).isEqualTo(ProductionStatus.FAILED);
        assertThat(dto.errorMessage()).isEqualTo("오류 발생");
        assertThat(dto.artifact()).isNull();
        assertThat(dto.artifacts()).isEmpty();
    }

    @Test
    @DisplayName("Job이 PROCESSING이면 status=PROCESSING, artifact 없음")
    void toDto_with_processing_job_returns_processing_status() {
        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(
                emptyArtifact(), processingJob(), artifactHandlerRegistry);

        assertThat(dto.status()).isEqualTo(ProductionStatus.PROCESSING);
        assertThat(dto.artifact()).isNull();
        assertThat(dto.errorMessage()).isNull();
    }

    @Test
    @DisplayName("PENDING Job은 status=PROCESSING으로 매핑된다")
    void toDto_pending_job_maps_to_processing() {
        JobEntity pendingJob = JobEntity.builder()
                .jobId("j-pending").idempotencyKey("k-pending").promptId(1L)
                .userId(1L).tenantId("t").commandType("TEXT").commandJson("{}")
                .build();

        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(
                emptyArtifact(), pendingJob, artifactHandlerRegistry);

        assertThat(dto.status()).isEqualTo(ProductionStatus.PROCESSING);
    }

    @Test
    @DisplayName("UNKNOWN Job은 status=PROCESSING으로 매핑된다")
    void toDto_unknown_job_maps_to_processing() {
        JobEntity unknownJob = JobEntity.builder()
                .jobId("j-unknown").idempotencyKey("k-unknown").promptId(1L)
                .userId(1L).tenantId("t").commandType("TEXT").commandJson("{}")
                .build();
        unknownJob.start();
        unknownJob.markAsUnknown("AI timeout");

        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(
                emptyArtifact(), unknownJob, artifactHandlerRegistry);

        assertThat(dto.status()).isEqualTo(ProductionStatus.PROCESSING);
    }

    // ===== Job 정보 없는 경우 (레거시 호환) =====

    @Test
    @DisplayName("Job 없이 artifact가 있으면 status=SUCCEEDED (레거시 호환)")
    void toDto_without_job_with_artifact_returns_succeeded() {
        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(
                artifactWithPrimaryTextDetail(), artifactHandlerRegistry);

        assertThat(dto.status()).isEqualTo(ProductionStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("Job 없이 artifact가 없으면 status=PROCESSING (레거시 호환)")
    void toDto_without_job_without_artifact_returns_processing() {
        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(
                emptyArtifact(), artifactHandlerRegistry);

        assertThat(dto.status()).isEqualTo(ProductionStatus.PROCESSING);
        assertThat(dto.artifact()).isNull();
    }
}
