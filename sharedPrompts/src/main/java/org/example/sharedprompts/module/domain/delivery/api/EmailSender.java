package org.example.sharedprompts.module.domain.delivery.api;

public interface EmailSender {
    DeliveryResult send(String emailContent, DeliveryContext context);
}

