package org.example.sharedprompts.module.domain.delivery.notion;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.delivery.api.DeliveryContext;
import org.example.sharedprompts.module.domain.delivery.api.DeliveryResult;
import org.example.sharedprompts.module.domain.delivery.api.DeliveryService;
import org.example.sharedprompts.module.domain.delivery.api.DeliveryType;
import org.example.sharedprompts.module.domain.delivery.api.NotionClient;
import org.example.sharedprompts.module.domain.delivery.exception.DeliveryException;
import org.example.sharedprompts.module.domain.production.api.ArtifactType;
import org.example.sharedprompts.module.domain.production.api.ProductionArtifact;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotionDeliveryService implements DeliveryService {
    
    private final NotionClient notionClient;
    
    @Override
    public DeliveryType getSupportedDeliveryType() {
        return DeliveryType.NOTION;
    }
    
    @Override
    public DeliveryResult deliver(
            ProductionArtifact artifact, 
            DeliveryContext context
    ) {
        // Notion은 TEXT 또는 FILE Artifact를 지원
        if (artifact.getType() != ArtifactType.TEXT && artifact.getType() != ArtifactType.FILE) {
            throw new DeliveryException("Notion delivery requires TEXT or FILE artifact");
        }
        
        String content = artifact.getLocation();
        String pageTitle = context.getAttribute("pageTitle", String.class);
        String parentPageId = context.getAttribute("parentPageId", String.class);
        
        // pageTitle은 필수 속성
        if (pageTitle == null || pageTitle.trim().isEmpty()) {
            throw new DeliveryException("Notion delivery requires pageTitle in context");
        }
        
        // parentPageId는 선택적 (null 허용)
        
        return notionClient.createPage(content, pageTitle, parentPageId, context);
    }
}

