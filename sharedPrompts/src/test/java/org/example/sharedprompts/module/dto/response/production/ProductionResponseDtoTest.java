package org.example.sharedprompts.module.dto.response.production;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.entity.production.StorageFormat;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionResponseDtoTest {

    @Test
    void testSuccessStatus() {
        ProductionArtifactDetailEntity detail = ProductionArtifactDetailEntity.builder()
                .artifactType(ArtifactType.TEXT)
                .storageType(StorageFormat.INLINE_TEXT)
                .content("Test content")
                .fileName("test.txt")
                .contentType("text/plain")
                .storageLocation("LOCAL")
                .build();

        ProductionArtifactEntity entity = ProductionArtifactEntity.builder()
                .id(1L)
                .userId(100L)
                .success(true)
                .startedAt(Instant.now())
                .completedAt(Instant.now())
                .build();
        entity.setDetail(detail);

        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(entity);

        assertThat(dto.status()).isEqualTo(ProductionStatus.SUCCESS);
        assertThat(dto.errorMessage()).isNull();
        assertThat(dto.artifact()).isNotNull();
        assertThat(dto.artifact().type()).isEqualTo(ArtifactType.TEXT);
    }

    @Test
    void testFailedStatus() {
        ProductionArtifactDetailEntity detail = ProductionArtifactDetailEntity.builder()
                .artifactType(ArtifactType.TEXT)
                .errorMessage("Processing failed")
                .build();

        ProductionArtifactEntity entity = ProductionArtifactEntity.builder()
                .id(1L)
                .userId(100L)
                .success(false)
                .startedAt(Instant.now())
                .completedAt(Instant.now())
                .build();
        entity.setDetail(detail);

        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(entity);

        assertThat(dto.status()).isEqualTo(ProductionStatus.FAILED);
        assertThat(dto.errorMessage()).isEqualTo("Processing failed");
        assertThat(dto.artifact()).isNull();
    }

    @Test
    void testProcessingStatus() {
        ProductionArtifactEntity entity = ProductionArtifactEntity.builder()
                .id(1L)
                .userId(100L)
                .success(true)
                .startedAt(Instant.now())
                .completedAt(null)
                .build();

        ProductionResponseDto dto = ProductionResponseDtoMapper.toDto(entity);

        assertThat(dto.status()).isEqualTo(ProductionStatus.PROCESSING);
        assertThat(dto.errorMessage()).isNull();
        assertThat(dto.artifact()).isNull();
    }
}

