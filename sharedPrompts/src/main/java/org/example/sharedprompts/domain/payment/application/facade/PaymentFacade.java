package org.example.sharedprompts.domain.payment.application.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.application.command.PaymentCommandService;
import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentConfirmResponse;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {
    
    private final PaymentCommandService paymentCommandService;
    private final PaymentWebhookFacade webhookFacade;
    private final PaymentRetryFacade retryFacade;
    
    @Transactional
    public PaymentResponseDto requestPayment(Long userId, PaymentRequestDto request) {
        return paymentCommandService.requestPayment(userId, request);
    }
    
    @Transactional(readOnly = true)
    public PaymentStatusResponseDto checkPaymentStatus(Long paymentId, Long userId) {
        return paymentCommandService.checkPaymentStatus(paymentId, userId);
    }

    @Transactional
    public PaymentResponseDto cancelPayment(Long userId, PaymentCancelRequestDto request) {
        return paymentCommandService.cancelPayment(userId, request);
    }

    @Transactional
    public PaymentResponseDto refundPayment(Long userId, PaymentRefundRequestDto request) {
        return paymentCommandService.refundPayment(userId, request);
    }

    @Transactional
    public PaymentConfirmResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        return paymentCommandService.confirmPayment(userId, request);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable) {
        return paymentCommandService.getPaymentHistory(userId, pageable);
    }

    @Transactional
    public Optional<Payment> handleWebhook(PaymentMethod paymentMethod, String payload, String signature) {
        return webhookFacade.handleWebhook(paymentMethod, payload, signature);
    }

    @Transactional
    public Optional<Payment> handleWebhook(
            PaymentMethod paymentMethod,
            String payload,
            String signature,
            Map<String, String> headers
    ) {
        return webhookFacade.handleWebhook(paymentMethod, payload, signature, headers);
    }
    
    @Transactional
    public PaymentResponseDto retryPayment(Long paymentId) {
        return retryFacade.retryPaymentAsResponse(paymentId);
    }
}

