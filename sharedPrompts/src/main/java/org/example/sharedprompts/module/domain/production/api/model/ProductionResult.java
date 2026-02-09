package org.example.sharedprompts.module.domain.production.api.model;

import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;

import java.time.Instant;

public interface ProductionResult {
    boolean isSuccess();
    String getErrorMessage();
    Instant getStartedAt();
    Instant getCompletedAt();
    ProductionArtifact getArtifact();
}

