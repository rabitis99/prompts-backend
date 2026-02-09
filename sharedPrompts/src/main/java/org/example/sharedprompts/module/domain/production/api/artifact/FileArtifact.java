package org.example.sharedprompts.module.domain.production.api.artifact;

public class FileArtifact implements ProductionArtifact {
    private final String filePath;
    
    public FileArtifact(String filePath) {
        this.filePath = filePath;
    }
    
    @Override
    public ArtifactType getType() {
        return ArtifactType.FILE;
    }
    
    @Override
    public String getLocation() {
        return filePath;
    }
}

