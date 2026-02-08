package org.example.sharedprompts.domain.production.api;

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

