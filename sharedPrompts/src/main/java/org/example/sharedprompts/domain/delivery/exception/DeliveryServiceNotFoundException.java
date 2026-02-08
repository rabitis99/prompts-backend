package org.example.sharedprompts.domain.delivery.exception;

import org.example.sharedprompts.domain.delivery.api.DeliveryType;

public class DeliveryServiceNotFoundException extends DeliveryException {
    public DeliveryServiceNotFoundException(DeliveryType deliveryType) {
        super("Delivery service not found for type: " + deliveryType);
    }
}

