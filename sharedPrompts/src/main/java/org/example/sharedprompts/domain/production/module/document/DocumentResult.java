package org.example.sharedprompts.domain.production.module.document;

import org.example.sharedprompts.domain.production.api.ProductionArtifact;
import org.example.sharedprompts.domain.production.api.ProductionResult;

import java.time.Instant;

public class DocumentResult implements ProductionResult {
    private final boolean success;
    private final String errorMessage;
    private final Instant startedAt;
    private final Instant completedAt;
    private final ProductionArtifact artifact;
    
    private DocumentResult(boolean success, String errorMessage, Instant startedAt, Instant completedAt, ProductionArtifact artifact) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.artifact = artifact;
    }
    
    public static DocumentResult success(ProductionArtifact artifact, Instant startedAt, Instant completedAt) {
        return new DocumentResult(true, null, startedAt, completedAt, artifact);
    }
    
    public static DocumentResult failure(String errorMessage, Instant startedAt, Instant completedAt) {
        return new DocumentResult(false, errorMessage, startedAt, completedAt, null);
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

