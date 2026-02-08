package org.example.sharedprompts.domain.production.api;

public class ImageArtifact implements ProductionArtifact {
    private final String imagePath;
    
    public ImageArtifact(String imagePath) {
        this.imagePath = imagePath;
    }
    
    @Override
    public ArtifactType getType() {
        return ArtifactType.IMAGE;
    }
    
    @Override
    public String getLocation() {
        return imagePath;
    }
}

