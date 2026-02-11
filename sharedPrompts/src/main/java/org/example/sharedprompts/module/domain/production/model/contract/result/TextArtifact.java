package org.example.sharedprompts.module.domain.production.model.contract.result;

public record TextArtifact(String content) implements ProductionArtifact {
    @Override
    public ArtifactType getType() {
        return ArtifactType.TEXT;
    }
    
    @Override
    public String getLocation() {
        return content;
    }
}

