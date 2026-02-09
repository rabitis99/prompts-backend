package org.example.sharedprompts.domain.delivery.api;

public interface NotionClient {
    DeliveryResult createPage(String content, String pageTitle, String parentPageId, DeliveryContext context);
}

