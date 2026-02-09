package org.example.sharedprompts.domain.delivery.api;

public interface BlogPublisher {
    DeliveryResult publish(String blogContent, String platform, DeliveryContext context);
}

