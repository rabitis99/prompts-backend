package org.example.sharedprompts.module.domain.delivery.blog;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.delivery.api.client.BlogPublisher;
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
public class BlogDeliveryService implements DeliveryService {
    
    private final BlogPublisher blogPublisher;
    
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
        
        if (platform == null || platform.isBlank()) {
            throw new DeliveryException("Blog delivery requires a platform to be specified");
        }
        
        return blogPublisher.publish(blogContent, platform, context);
    }
}

