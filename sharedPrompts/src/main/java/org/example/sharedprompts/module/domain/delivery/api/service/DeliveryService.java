package org.example.sharedprompts.module.domain.delivery.api.service;

import org.example.sharedprompts.module.domain.delivery.exception.DeliveryException;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryContext;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryResult;
import org.example.sharedprompts.module.domain.delivery.api.type.DeliveryType;
import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;

public interface DeliveryService {
    DeliveryType getSupportedDeliveryType();
    DeliveryResult deliver(ProductionArtifact artifact, DeliveryContext context) 
            throws DeliveryException;
}

