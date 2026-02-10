package org.example.sharedprompts.module.domain.production.api.model;

import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;

import java.time.Instant;

public class DefaultModuleProductionResult implements ModuleProductionResult {
    private final boolean success;
    private final String errorMessage;
    private final Instant startedAt;
    private final Instant completedAt;
    private final ProductionArtifact artifact;

    private DefaultModuleProductionResult(boolean success, String errorMessage,
            Instant startedAt, Instant completedAt, ProductionArtifact artifact) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.artifact = artifact;
    }

    public static DefaultModuleProductionResult success(ProductionArtifact artifact,
            Instant startedAt, Instant completedAt) {
        return new DefaultModuleProductionResult(true, null, startedAt, completedAt, artifact);
    }

    public static DefaultModuleProductionResult failure(String errorMessage,
            Instant startedAt, Instant completedAt) {
        return new DefaultModuleProductionResult(false, errorMessage, startedAt, completedAt, null);
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
}

