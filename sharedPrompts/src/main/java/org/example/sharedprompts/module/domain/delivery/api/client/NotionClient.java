package org.example.sharedprompts.module.domain.delivery.api.client;

import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryContext;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryResult;

public interface NotionClient {
    DeliveryResult createPage(String content, String pageTitle, String parentPageId, DeliveryContext context);
}

