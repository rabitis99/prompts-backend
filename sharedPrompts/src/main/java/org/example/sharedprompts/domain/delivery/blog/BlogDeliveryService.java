package org.example.sharedprompts.domain.delivery.blog;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.delivery.api.DeliveryContext;
import org.example.sharedprompts.domain.delivery.api.DeliveryResult;
import org.example.sharedprompts.domain.delivery.api.DeliveryService;
import org.example.sharedprompts.domain.delivery.api.DeliveryType;
import org.example.sharedprompts.domain.delivery.exception.DeliveryException;
import org.example.sharedprompts.domain.production.api.ArtifactType;
import org.example.sharedprompts.domain.production.api.ProductionArtifact;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlogDeliveryService implements DeliveryService {
    
    // TODO: infra 계층의 BlogPublisher 주입 필요
    // private final BlogPublisher blogPublisher;
    
    @Override
    public DeliveryType getSupportedDeliveryType() {
        return DeliveryType.BLOG;
    }
    
    @Override
    public DeliveryResult deliver(
            ProductionArtifact artifact, 
            DeliveryContext context
    ) {
        if (artifact.getType() != ArtifactType.TEXT) {
            throw new DeliveryException("Blog delivery requires TEXT artifact");
        }
        
        String blogContent = artifact.getLocation();
        String platform = context.getPlatform();
        
        // TODO: blogPublisher.publish() 호출하여 블로그 게시
        // return blogPublisher.publish(blogContent, platform, context);
        
        throw new UnsupportedOperationException("TODO: Implement blog publishing logic");
    }
}

