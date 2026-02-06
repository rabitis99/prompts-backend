package org.example.sharedprompts.domain.payment.application.command.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.application.dto.response.CancelResult;
import org.example.sharedprompts.domain.payment.application.dto.response.RefundResult;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.exception.DuplicateOrderIdException;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.infrastructure.idempotency.IdempotencyService;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentExecutionServiceTransaction {

    private final PaymentExecutionTemplate executionTemplate;
    private final PaymentExecutionValidator validator;
    private final DuplicateOrderIdHandler duplicateOrderIdHandler;
    private final PaymentJpaAdapter paymentJpaAdapter;
    private final IdempotencyService idempotencyService;
    private final PaymentResultEventPublisher resultEventPublisher;

    public Payment executePayment(Payment payment, BigDecimal actualAmount) {
        return executePayment(payment, actualAmount, Collections.emptyMap());
    }

    public Payment executePayment(Payment payment, BigDecimal actualAmount, Map<String, String> additionalParams) {
        return executePaymentInTransaction(payment.getId(), actualAmount, additionalParams);
    }

    public Payment executePaymentInTransaction(Long paymentId, BigDecimal actualAmount, Map<String, String> additionalParams) {
        Payment payment = loadPaymentForExecution(paymentId);
        
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment가 이미 완료 상태: paymentId={}, externalPaymentId={}",
                    payment.getId(), payment.getExternalPaymentId());
            return payment;
        }

        validator.validatePaymentForExecution(payment);

        try {
            String orderIdForProvider = validator.determineOrderId(payment, additionalParams);
            String idempotencyKey = idempotencyService.generateForPayment(payment);
            
            PaymentResult result = executionTemplate.callProvider(
                    payment,
                    provider -> provider.confirmPayment(
                            payment.getExternalPaymentId(),
                            orderIdForProvider,
                            actualAmount,
                            payment.getCurrency(),
                            idempotencyKey,
                            String.valueOf(payment.getUser().getId()),
                            additionalParams != null ? additionalParams : Collections.emptyMap()
                    )
            );

            return resultEventPublisher.publishPaymentResult(paymentId, result, actualAmount, idempotencyKey);
        } catch (DuplicateOrderIdException e) {
            return duplicateOrderIdHandler.handleDuplicateOrderIdException(paymentId, e, actualAmount);
        }
    }

    private Payment loadPaymentForExecution(Long paymentId) {
        return paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    @Transactional
    public Payment handleDuplicateOrderIdException(
            Long paymentId,
            DuplicateOrderIdException e,
            BigDecimal actualAmount
    ) {
        return duplicateOrderIdHandler.handleDuplicateOrderIdException(paymentId, e, actualAmount);
    }

    public Payment executeCancel(Payment payment, String reason) {
        validator.validateExternalPaymentId(payment);
        
        String idempotencyKey = idempotencyService.generateForCancel(payment);
        
        CancelResult result = executionTemplate.callProvider(
                payment,
                provider -> provider.cancelPayment(payment.getExternalPaymentId(), reason, idempotencyKey)
        );

        return resultEventPublisher.publishCancelResult(payment.getId(), result);
    }
    
    public Payment executeRefund(Payment payment, BigDecimal refundAmount, String reason) {
        validator.validateExternalPaymentId(payment);
        
        String idempotencyKey = idempotencyService.generateForRefund(payment);
        
        RefundResult result = executionTemplate.callProvider(
                payment,
                provider -> provider.refundPayment(payment.getExternalPaymentId(), refundAmount, reason, idempotencyKey)
        );

        return resultEventPublisher.publishRefundResult(payment.getId(), result, refundAmount, idempotencyKey);
    }
}

