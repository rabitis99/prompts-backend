package org.example.sharedprompts.domain.production.api;

public interface ProductionArtifact {
    ArtifactType getType();
    String getLocation();
}

