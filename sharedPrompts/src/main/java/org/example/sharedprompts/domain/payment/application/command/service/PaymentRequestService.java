package org.example.sharedprompts.domain.payment.application.command.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.PaymentValidationService;
import org.example.sharedprompts.domain.payment.application.command.service.amount.PaymentAmountProcessingService;
import org.example.sharedprompts.domain.payment.application.command.service.request.PaymentCreator;
import org.example.sharedprompts.domain.payment.application.command.service.request.PaymentPreparationHandler;
import org.example.sharedprompts.domain.payment.application.dto.AmountProcessingResult;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRequestService {

    private final PaymentValidationService validationService;
    private final PaymentAmountProcessingService amountProcessingService;
    private final PaymentCreator paymentCreator;
    private final PaymentPreparationHandler preparationHandler;
    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentLoggingService loggingService;

    @Transactional
    public Payment processRequest(Long userId, PaymentRequestDto request, org.example.sharedprompts.domain.user.User user) {
        log.info("결제 요청 시작: userId={}, paymentMethod={}, amount={}",
                userId, request.getPaymentMethod(), request.getAmount());

        validationService.validateDailyLimit(userId, user.getTier());

        AmountProcessingResult amountResult = amountProcessingService.processPaymentAmount(
                userId,
                request.getAmount(),
                request.getCurrency(),
                request.getUsePointAmount()
        );

        Payment payment = paymentCreator.createPayment(user, request, amountResult);

        // 결제 준비를 먼저 처리하고 메타데이터를 포함하여 한 번에 저장
        payment = preparationHandler.processPreparationIfNeeded(payment, amountResult, userId, request);

        payment = paymentJpaAdapter.save(payment);
        loggingService.logPaymentRequest(payment);

        log.info("결제 요청 완료: paymentId={}, userId={}, paymentMethod={}",
                payment.getId(), userId, request.getPaymentMethod());

        return payment;
    }
}

