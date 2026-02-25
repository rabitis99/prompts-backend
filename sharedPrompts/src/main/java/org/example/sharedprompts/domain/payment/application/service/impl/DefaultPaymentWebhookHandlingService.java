package org.example.sharedprompts.domain.payment.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.port.in.command.PaymentWebhookCommand;
import org.example.sharedprompts.domain.payment.application.port.in.usecase.PaymentWebhookUseCase;
import org.example.sharedprompts.domain.payment.application.port.out.webhook.PaymentWebhookProcessingPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Transactional
public class DefaultPaymentWebhookHandlingService implements PaymentWebhookUseCase {

    private final PaymentWebhookProcessingPort webhookProcessingPort;

    @Override
    public Optional<Payment> handleWebhook(PaymentWebhookCommand command) {
        log.info("웹훅 처리 시작: paymentMethod={}", command.getPaymentMethod());
        Map<String, String> headers = command.getHeaders() != null ? command.getHeaders() : Collections.emptyMap();
        return webhookProcessingPort.processWebhook(
                command.getPaymentMethod(),
                command.getPayload(),
                command.getSignature(),
                headers
        );
    }
}
