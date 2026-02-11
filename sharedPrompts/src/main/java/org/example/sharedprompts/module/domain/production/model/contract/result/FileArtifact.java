package org.example.sharedprompts.module.domain.production.model.contract.result;

public record FileArtifact(String filePath) implements ProductionArtifact {
    @Override
    public ArtifactType getType() {
        return ArtifactType.FILE;
    }
    
    @Override
    public String getLocation() {
        return filePath;
    }
}

