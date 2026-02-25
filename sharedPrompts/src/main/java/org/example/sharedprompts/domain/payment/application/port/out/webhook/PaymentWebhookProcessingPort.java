package org.example.sharedprompts.domain.payment.application.port.out.webhook;

import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;

import java.util.Map;
import java.util.Optional;

public interface PaymentWebhookProcessingPort {

    Optional<Payment> processWebhook(
            PaymentMethod paymentMethod,
            String payload,
            String signature,
            Map<String, String> headers
    );
}
