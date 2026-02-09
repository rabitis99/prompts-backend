package org.example.sharedprompts.module.domain.delivery.api;

import org.example.sharedprompts.module.domain.delivery.exception.DeliveryException;
import org.example.sharedprompts.module.domain.production.api.ProductionArtifact;

public interface DeliveryService {
    DeliveryType getSupportedDeliveryType();
    DeliveryResult deliver(ProductionArtifact artifact, DeliveryContext context) 
            throws DeliveryException;
}

