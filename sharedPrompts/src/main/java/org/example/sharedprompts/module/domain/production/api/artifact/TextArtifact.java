package org.example.sharedprompts.module.domain.production.api.artifact;

public class TextArtifact implements ProductionArtifact {
    private final String content;
    
    public TextArtifact(String content) {
        this.content = content;
    }
    
    @Override
    public ArtifactType getType() {
        return ArtifactType.TEXT;
    }
    
    @Override
    public String getLocation() {
        return content;
    }
}

