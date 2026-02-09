package org.example.sharedprompts.module.domain.delivery.github;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.delivery.api.client.GitHubClient;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryContext;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryResult;
import org.example.sharedprompts.module.domain.delivery.api.service.DeliveryService;
import org.example.sharedprompts.module.domain.delivery.api.type.DeliveryType;
import org.example.sharedprompts.module.domain.delivery.exception.DeliveryException;
import org.example.sharedprompts.module.domain.production.api.artifact.ArtifactType;
import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GitHubDeliveryService implements DeliveryService {
    
    private final GitHubClient gitHubClient;
    
    @Override
    public DeliveryType getSupportedDeliveryType() {
        return DeliveryType.GITHUB;
    }
    
    @Override
    public DeliveryResult deliver(
            ProductionArtifact artifact, 
            DeliveryContext context
    ) {
        // GitHub는 FILE 또는 IMAGE Artifact를 지원
        if (artifact.getType() != ArtifactType.FILE && artifact.getType() != ArtifactType.IMAGE) {
            throw new DeliveryException("GitHub delivery requires FILE or IMAGE artifact");
        }
        
        String filePath = artifact.getLocation();
        String action = context.getAttribute("action", String.class); // "pr", "commit", "issue" 등
        
        if (action == null) {
            action = "commit"; // 기본값
        }
        
        return gitHubClient.upload(filePath, action, context);
    }
}

