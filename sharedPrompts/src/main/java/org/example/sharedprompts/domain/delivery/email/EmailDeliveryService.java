package org.example.sharedprompts.domain.delivery.email;

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
public class EmailDeliveryService implements DeliveryService {
    
    // TODO: infra 계층의 EmailSender 주입 필요
    // private final EmailSender emailSender;
    
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
        
        // TODO: emailSender.send() 호출하여 이메일 전송
        // return emailSender.send(emailContent, context);
        
        throw new UnsupportedOperationException("TODO: Implement email sending logic");
    }
}

