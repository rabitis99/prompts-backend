package org.example.sharedprompts.module.domain.delivery.api;

public interface BlogPublisher {
    DeliveryResult publish(String blogContent, String platform, DeliveryContext context);
}

