package org.example.sharedprompts.infra.delivery.blog;

import org.example.sharedprompts.domain.delivery.api.BlogPublisher;
import org.example.sharedprompts.domain.delivery.api.DeliveryContext;
import org.example.sharedprompts.domain.delivery.api.DeliveryResult;
import org.example.sharedprompts.domain.delivery.api.DefaultDeliveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 블로그 플랫폼에 게시하는 Publisher 구현체.
 */
@Component
public class BlogPublisherImpl implements BlogPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(BlogPublisherImpl.class);
    
    /**
     * 블로그 콘텐츠를 플랫폼에 게시한다.
     */
    public DeliveryResult publish(String blogContent, String platform, DeliveryContext context) {
        if (blogContent == null) {
            throw new IllegalArgumentException("blogContent must not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        
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

