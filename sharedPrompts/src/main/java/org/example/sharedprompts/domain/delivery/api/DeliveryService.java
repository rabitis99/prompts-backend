package org.example.sharedprompts.domain.delivery.api;

import org.example.sharedprompts.domain.delivery.exception.DeliveryException;
import org.example.sharedprompts.domain.production.api.ProductionArtifact;

public interface DeliveryService {
    DeliveryType getSupportedDeliveryType();
    DeliveryResult deliver(ProductionArtifact artifact, DeliveryContext context) 
            throws DeliveryException;
}

