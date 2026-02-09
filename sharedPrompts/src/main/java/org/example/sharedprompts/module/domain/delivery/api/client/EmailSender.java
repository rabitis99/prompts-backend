package org.example.sharedprompts.module.domain.delivery.api.client;

import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryContext;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryResult;

public interface EmailSender {
    DeliveryResult send(String emailContent, DeliveryContext context);
}

