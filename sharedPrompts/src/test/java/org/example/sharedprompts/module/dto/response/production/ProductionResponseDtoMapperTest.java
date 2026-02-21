package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.example.sharedprompts.module.domain.production.model.production.ProductionStatus;
import org.example.sharedprompts.module.domain.production.service.artifact.ArtifactHandlerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductionResponseDtoMapper 상태 매핑 테스트
 *
 * NOTE: ArtifactDto가 sealed interface라 Mockito로 mock 불가.
 * primary artifact가 없는 시나리오로 ArtifactDtoMapper.toDto() 미호출 경로만 테스트합니다.
 * ArtifactHandler 연동은 통합 테스트에서 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductionResponseDtoMapper 상태 매핑 테스트")
class ProductionResponseDtoMapperTest {

    @Mock
    private ArtifactHandlerRegistry artifactHandlerRegistry;

    private ProductionArtifactEntity emptyArtifact() {
        return ProductionArtifactEntity.builder()
                .jobId(1L).tenantId("tenant-1").userId(100L)
                .commandType(ProductionCommandType.TEXT)
                .build();
    }

    /**
     * artifacts가 있지만 primary 없음 → status=SUCCEEDED, ArtifactDtoMapper 미호출
     */
    private ProductionArtifactEntity artifactWithNonPrimaryDetail() {
        ProductionArtifactEntity entity = ProductionArtifactEntity.builder()
                .jobId(1L).tenantId("t").userId(100L).commandType(ProductionCommandType.TEXT)
                .build();
        entity.addArtifact(ProductionArtifactDetailEntity.builder()
                .artifactType(ArtifactType.TEXT).content("내용")
                .fileName("output.txt").contentType("text/plain").build());
        return entity;
    }

    private JobEntity baseJob(String jobId, String key) {
        return JobEntity.builder()
                .jobId(jobId).idempotencyKey(key).promptId(1L)
                .userId(100L).tenantId("tenant-1").commandType("TEXT").commandJson("{}")
                .build();
    }

    private JobEntity succeededJob() {
        JobEntity job = baseJob("j-s", "k-s");
        job.start();
        job.complete("artifact-1");
        return job;
    }

    private JobEntity failedJob() {
        JobEntity job = baseJob("j-f", "k-f");
        job.start();
        job.fail("처리 오류");
        return job;
    }

    // ===== Job 기반 상태 매핑 =====

    @Test
    @DisplayName("Job이 SUCCEEDED이면 status=SUCCEEDED, startedAt/completedAt 포함")
    void toDto_with_succeeded_job_returns_succeeded_status() {
        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(
                emptyArtifact(), succeededJob(), artifactHandlerRegistry);

        assertThat(dto.status()).isEqualTo(ProductionStatus.SUCCEEDED);
        assertThat(dto.errorMessage()).isNull();
        assertThat(dto.startedAt()).isNotNull();
        assertThat(dto.completedAt()).isNotNull();
    }

    @Test
    @DisplayName("Job이 FAILED이면 status=FAILED, errorMessage 포함")
    void toDto_with_failed_job_returns_failed_status_with_error_message() {
        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(
                emptyArtifact(), failedJob(), artifactHandlerRegistry);

        assertThat(dto.status()).isEqualTo(ProductionStatus.FAILED);
        assertThat(dto.errorMessage()).isEqualTo("처리 오류");
        assertThat(dto.artifact()).isNull();
        assertThat(dto.artifacts()).isEmpty();
    }

    @Test
    @DisplayName("Job이 PROCESSING이면 status=PROCESSING, artifact 없음")
    void toDto_with_processing_job_returns_processing_status() {
        JobEntity processingJob = baseJob("j-p", "k-p");
        processingJob.start();

        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(
                emptyArtifact(), processingJob, artifactHandlerRegistry);

        assertThat(dto.status()).isEqualTo(ProductionStatus.PROCESSING);
        assertThat(dto.artifact()).isNull();
        assertThat(dto.errorMessage()).isNull();
    }

    @Test
    @DisplayName("PENDING/RETRYING/UNKNOWN Job은 모두 status=PROCESSING으로 매핑된다")
    void toDto_non_terminal_jobs_map_to_processing() {
        // PENDING
        assertThat(ProductionResponseDtoMapper.toDto(emptyArtifact(), baseJob("j1", "k1"), artifactHandlerRegistry).status())
                .isEqualTo(ProductionStatus.PROCESSING);

        // RETRYING
        JobEntity retryingJob = baseJob("j2", "k2");
        retryingJob.start();
        retryingJob.fail("실패");
        retryingJob.markAsRetrying();
        assertThat(ProductionResponseDtoMapper.toDto(emptyArtifact(), retryingJob, artifactHandlerRegistry).status())
                .isEqualTo(ProductionStatus.PROCESSING);

        // UNKNOWN
        JobEntity unknownJob = baseJob("j3", "k3");
        unknownJob.start();
        unknownJob.markAsUnknown("timeout");
        assertThat(ProductionResponseDtoMapper.toDto(emptyArtifact(), unknownJob, artifactHandlerRegistry).status())
                .isEqualTo(ProductionStatus.PROCESSING);
    }

    // ===== 레거시 호환 (Job 없는 경우) =====

    @Test
    @DisplayName("Job 없이 artifact가 있으면 status=SUCCEEDED (레거시 호환)")
    void toDto_without_job_with_artifact_returns_succeeded() {
        // primary 없는 detail → ArtifactDtoMapper.toDto() 미호출, artifact=null
        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(
                artifactWithNonPrimaryDetail(), artifactHandlerRegistry);

        assertThat(dto.status()).isEqualTo(ProductionStatus.SUCCEEDED);
        assertThat(dto.artifact()).isNull();
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
