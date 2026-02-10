package org.example.sharedprompts.module.domain.production.api.model;

import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;

import java.time.Instant;

public class DefaultProductionResult implements ProductionResult {
    private final boolean success;
    private final String errorMessage;
    private final Instant startedAt;
    private final Instant completedAt;
    private final ProductionArtifact artifact;
    private final Long artifactId;

    private DefaultProductionResult(boolean success, String errorMessage,
            Instant startedAt, Instant completedAt, ProductionArtifact artifact, Long artifactId) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.artifact = artifact;
        this.artifactId = artifactId;
    }

    public static DefaultProductionResult success(ProductionArtifact artifact,
            Instant startedAt, Instant completedAt, Long artifactId) {
        return new DefaultProductionResult(true, null, startedAt, completedAt, artifact, artifactId);
    }

    public static DefaultProductionResult failure(String errorMessage,
            Instant startedAt, Instant completedAt, Long artifactId) {
        return new DefaultProductionResult(false, errorMessage, startedAt, completedAt, null, artifactId);
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public Instant getStartedAt() {
        return startedAt;
    }

    @Override
    public Instant getCompletedAt() {
        return completedAt;
    }

    @Override
    public ProductionArtifact getArtifact() {
        return artifact;
    }

    @Override
    public Long getArtifactId() {
        return artifactId;
    }
    
    public static DefaultProductionResult fromModuleResult(ModuleProductionResult moduleResult, Long artifactId) {
        if (moduleResult.isSuccess()) {
            return new DefaultProductionResult(
                true,
                null,
                moduleResult.getStartedAt(),
                moduleResult.getCompletedAt(),
                moduleResult.getArtifact(),
                artifactId
            );
        } else {
            return new DefaultProductionResult(
                false,
                moduleResult.getErrorMessage(),
                moduleResult.getStartedAt(),
                moduleResult.getCompletedAt(),
                null,
                artifactId
            );
        }
    }
}

