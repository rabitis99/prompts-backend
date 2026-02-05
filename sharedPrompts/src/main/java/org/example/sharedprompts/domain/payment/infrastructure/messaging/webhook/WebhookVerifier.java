package org.example.sharedprompts.domain.payment.infrastructure.messaging.webhook;

import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;

import java.util.Map;

public interface WebhookVerifier {

    PaymentMethod getPaymentMethod();

    boolean verify(String payload, String signature);

    default boolean verify(String payload, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return verify(payload, (String) null);
        }

        String signature = null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase("signature")) {
                signature = entry.getValue();
                break;
            }
        }

        return verify(payload, signature);
    }
}

