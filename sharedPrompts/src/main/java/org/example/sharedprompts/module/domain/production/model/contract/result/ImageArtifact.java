package org.example.sharedprompts.module.domain.production.model.contract.result;

public record ImageArtifact(String imagePath) implements ProductionArtifact {
    @Override
    public ArtifactType getType() {
        return ArtifactType.IMAGE;
    }
    
    @Override
    public String getLocation() {
        return imagePath;
    }
}

