package org.example.sharedprompts.infra.delivery.blog;

import org.example.sharedprompts.domain.delivery.api.DeliveryContext;
import org.example.sharedprompts.domain.delivery.api.DeliveryResult;
import org.example.sharedprompts.domain.delivery.api.DefaultDeliveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 블로그 플랫폼에 게시하는 Publisher.
 * Delivery 계층에서 사용되며, 외부 API 호출을 담당한다.
 * 
 * 현재는 기본 구현으로, 실제 플랫폼 연동은 추후 구현 예정.
 */
@Component
public class BlogPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(BlogPublisher.class);
    
    /**
     * 블로그 콘텐츠를 플랫폼에 게시한다.
     * 
     * @param blogContent 게시할 블로그 콘텐츠
     * @param platform 플랫폼 (velog, tistory, medium 등)
     * @param context Delivery 컨텍스트
     * @return Delivery 결과
     */
    public DeliveryResult publish(String blogContent, String platform, DeliveryContext context) {
        log.info("블로그 게시 시도: platform={}, userId={}, contentLength={}", 
                platform, context.getUserId(), blogContent.length());
        
        // TODO: 실제 플랫폼 API 연동 구현
        // - Velog API 연동
        // - Tistory API 연동
        // - Medium API 연동
        
        // 현재는 시뮬레이션으로 성공 반환
        log.info("블로그 게시 완료 (시뮬레이션): platform={}", platform);
        return DefaultDeliveryResult.success();
    }
}

