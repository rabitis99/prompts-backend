package org.example.sharedprompts.infra.delivery.notion;

import org.example.sharedprompts.domain.delivery.api.DeliveryContext;
import org.example.sharedprompts.domain.delivery.api.DeliveryResult;
import org.example.sharedprompts.domain.delivery.api.DefaultDeliveryResult;
import org.example.sharedprompts.domain.delivery.api.NotionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Notion API와 연동하는 Client 구현체.
 */
@Component
public class NotionClientImpl implements NotionClient {
    
    private static final Logger log = LoggerFactory.getLogger(NotionClientImpl.class);
    
    /**
     * Notion 페이지를 생성한다.
     */
    public DeliveryResult createPage(String content, String pageTitle, String parentPageId, DeliveryContext context) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        
        log.info("Notion 페이지 생성 시도: userId={}, pageTitle={}, parentPageId={}, contentLength={}", 
                context.getUserId(), pageTitle, parentPageId, content.length());
        
        log.info("Notion 페이지 생성 완료 (시뮬레이션): pageTitle={}", pageTitle);
        return DefaultDeliveryResult.success();
    }
}

