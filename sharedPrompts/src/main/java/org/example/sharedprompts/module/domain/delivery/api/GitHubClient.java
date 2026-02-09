package org.example.sharedprompts.module.domain.delivery.api;

public interface GitHubClient {
    DeliveryResult upload(String filePath, String action, DeliveryContext context);
}

