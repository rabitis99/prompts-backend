package org.example.sharedprompts.domain.delivery.notion;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.delivery.api.DeliveryContext;
import org.example.sharedprompts.domain.delivery.api.DeliveryResult;
import org.example.sharedprompts.domain.delivery.api.DeliveryService;
import org.example.sharedprompts.domain.delivery.api.DeliveryType;
import org.example.sharedprompts.domain.delivery.exception.DeliveryException;
import org.example.sharedprompts.domain.production.api.ArtifactType;
import org.example.sharedprompts.domain.production.api.ProductionArtifact;
import org.example.sharedprompts.infra.delivery.notion.NotionClient;
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
        
        return notionClient.createPage(content, pageTitle, parentPageId, context);
    }
}

