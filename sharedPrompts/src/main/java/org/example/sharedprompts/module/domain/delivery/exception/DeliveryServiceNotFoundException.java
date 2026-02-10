package org.example.sharedprompts.module.domain.delivery.exception;

import org.example.sharedprompts.module.domain.delivery.api.type.DeliveryType;

public class DeliveryServiceNotFoundException extends DeliveryException {
    public DeliveryServiceNotFoundException(DeliveryType deliveryType) {
        super("Delivery service not found for type: " + deliveryType);
    }
}

