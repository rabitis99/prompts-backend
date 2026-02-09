package org.example.sharedprompts.infra.delivery.notion;

import org.example.sharedprompts.domain.delivery.api.DeliveryContext;
import org.example.sharedprompts.domain.delivery.api.DeliveryResult;
import org.example.sharedprompts.domain.delivery.api.DefaultDeliveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Notion API와 연동하는 Client.
 * Delivery 계층에서 사용되며, 외부 API 호출을 담당한다.
 * 
 * 현재는 기본 구현으로, 실제 Notion API 연동은 추후 구현 예정.
 */
@Component
public class NotionClient {
    
    private static final Logger log = LoggerFactory.getLogger(NotionClient.class);
    
    /**
     * Notion 페이지를 생성한다.
     * 
     * @param content 페이지 내용
     * @param pageTitle 페이지 제목
     * @param parentPageId 부모 페이지 ID (선택적)
     * @param context Delivery 컨텍스트
     * @return Delivery 결과
     */
    public DeliveryResult createPage(String content, String pageTitle, String parentPageId, DeliveryContext context) {
        log.info("Notion 페이지 생성 시도: userId={}, pageTitle={}, parentPageId={}, contentLength={}", 
                context.getUserId(), pageTitle, parentPageId, content.length());
        
        // TODO: 실제 Notion API 연동 구현
        // - Notion API v1 사용
        // - 인증 토큰 관리
        // - 페이지 생성 및 콘텐츠 추가
        // - 마크다운/HTML을 Notion 블록 형식으로 변환
        
        // 현재는 시뮬레이션으로 성공 반환
        log.info("Notion 페이지 생성 완료 (시뮬레이션): pageTitle={}", pageTitle);
        return DefaultDeliveryResult.success();
    }
}

