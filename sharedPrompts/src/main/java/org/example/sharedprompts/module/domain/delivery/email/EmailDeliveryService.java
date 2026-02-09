package org.example.sharedprompts.module.domain.delivery.email;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.delivery.api.DeliveryContext;
import org.example.sharedprompts.module.domain.delivery.api.DeliveryResult;
import org.example.sharedprompts.module.domain.delivery.api.DeliveryService;
import org.example.sharedprompts.module.domain.delivery.api.DeliveryType;
import org.example.sharedprompts.module.domain.delivery.api.EmailSender;
import org.example.sharedprompts.module.domain.delivery.exception.DeliveryException;
import org.example.sharedprompts.module.domain.production.api.ArtifactType;
import org.example.sharedprompts.module.domain.production.api.ProductionArtifact;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailDeliveryService implements DeliveryService {
    
    private final EmailSender emailSender;
    
    @Override
    public DeliveryType getSupportedDeliveryType() {
        return DeliveryType.EMAIL;
    }
    
    @Override
    public DeliveryResult deliver(
            ProductionArtifact artifact, 
            DeliveryContext context
    ) {
        if (artifact.getType() != ArtifactType.TEXT) {
            throw new DeliveryException("Email delivery requires TEXT artifact");
        }
        
        String emailContent = artifact.getLocation();
        
        return emailSender.send(emailContent, context);
    }
}

