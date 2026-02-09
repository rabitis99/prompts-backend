package org.example.sharedprompts.module.domain.delivery.notion;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.delivery.api.client.NotionClient;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryContext;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryResult;
import org.example.sharedprompts.module.domain.delivery.api.service.DeliveryService;
import org.example.sharedprompts.module.domain.delivery.api.type.DeliveryType;
import org.example.sharedprompts.module.domain.delivery.exception.DeliveryException;
import org.example.sharedprompts.module.domain.production.api.artifact.ArtifactType;
import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
public class NotionDeliveryService implements DeliveryService {
    
    private static final Logger log = LoggerFactory.getLogger(NotionDeliveryService.class);
    
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
        
        String content;
        if (artifact.getType() == ArtifactType.TEXT) {
            // TEXT Artifact: getLocation()은 실제 텍스트 콘텐츠를 반환
            content = artifact.getLocation();
        } else {
            // FILE Artifact: getLocation()은 파일 경로를 반환하므로 파일 내용을 읽어야 함
            String filePath = artifact.getLocation();
            if (filePath == null) {
                throw new DeliveryException("File artifact location is null");
            }
            try {
                Path path = Paths.get(filePath);
                content = Files.readString(path);
            } catch (IOException e) {
                log.error("Failed to read file content: {}", filePath, e);
                throw new DeliveryException("Failed to read file content for delivery", e);
            }
        }
        
        // content null 체크
        if (content == null) {
            throw new DeliveryException("Content is null for artifact type: " + artifact.getType());
        }
        
        String pageTitle = context.getAttribute("pageTitle", String.class);
        String parentPageId = context.getAttribute("parentPageId", String.class);
        
        // pageTitle은 필수 속성
        if (pageTitle == null || pageTitle.isBlank()) {
            throw new DeliveryException("Notion delivery requires pageTitle in context");
        }
        
        // parentPageId는 선택적 (null 허용)
        
        return notionClient.createPage(content, pageTitle, parentPageId, context);
    }
}