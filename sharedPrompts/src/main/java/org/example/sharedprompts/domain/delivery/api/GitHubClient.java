package org.example.sharedprompts.domain.delivery.api;

public interface GitHubClient {
    DeliveryResult upload(String filePath, String action, DeliveryContext context);
}

