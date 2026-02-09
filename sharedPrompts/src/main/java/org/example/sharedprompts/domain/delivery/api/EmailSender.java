package org.example.sharedprompts.domain.delivery.api;

public interface EmailSender {
    DeliveryResult send(String emailContent, DeliveryContext context);
}

