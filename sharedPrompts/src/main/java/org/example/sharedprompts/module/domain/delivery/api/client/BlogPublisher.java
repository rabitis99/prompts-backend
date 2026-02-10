package org.example.sharedprompts.module.domain.delivery.api.client;

import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryContext;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryResult;

public interface BlogPublisher {
    DeliveryResult publish(String blogContent, String platform, DeliveryContext context);
}

