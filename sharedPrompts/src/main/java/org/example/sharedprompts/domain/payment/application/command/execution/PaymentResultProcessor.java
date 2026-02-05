package org.example.sharedprompts.domain.payment.application.command.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.application.dto.response.CancelResult;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.application.dto.response.RefundResult;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.domain.service.PaymentValidator;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentResultProcessor {

    private final PaymentValidator paymentValidator;
    private final PaymentJpaAdapter paymentJpaAdapter;

    public void applyPaymentResult(Payment payment, PaymentResult result, BigDecimal actualAmount) {
        if (result.getExternalPaymentId() != null) {
            payment.updateExternalPaymentId(result.getExternalPaymentId());
        }

        try {
            paymentValidator.validatePaymentResult(payment, result, actualAmount);
            
            if (result.isSuccess()) {
                markPaymentSuccess(payment, result.getExternalPaymentId());
            } else {
                markPaymentFailed(payment, result.getFailureReason() != null 
                        ? result.getFailureReason() 
                        : "결제 승인 실패");
            }
        } catch (ApiException e) {
            log.error("결제 검증 실패 - 외부 결제는 완료되었으나 검증 불일치: paymentId={}, externalPaymentId={}, error={}",
                    payment.getId(), result.getExternalPaymentId(), e.getMessage());
            markPaymentFailed(payment, "검증 실패: " + e.getMessage());
            paymentJpaAdapter.save(payment);
            throw e;
        }
    }

    public void applyCancelResult(Payment payment, CancelResult result) {
        if (!result.isSuccess()) {
            throw new ApiException(ErrorCode.PAYMENT_CANCEL_FAILED, "결제 취소 실패");
        }

        markPaymentCanceled(payment);
        
        if (result.getOriginalAmount() != null && result.getTaxFreeAmount() != null) {
            payment.updateOriginalAmounts(result.getOriginalAmount(), result.getTaxFreeAmount());
        }
    }

    public void applyRefundResult(Payment payment, RefundResult result, BigDecimal requestedRefundAmount) {
        if (!result.isSuccess()) {
            throw new ApiException(ErrorCode.PAYMENT_REFUND_FAILED, "결제 환불 실패");
        }

        BigDecimal actualRefundedAmount = result.getRefundedAmount() != null 
                ? result.getRefundedAmount() 
                : requestedRefundAmount;
        markPaymentRefunded(payment, actualRefundedAmount);
    }

    public void markPaymentSuccess(Payment payment, String externalPaymentId) {
        payment.markSuccess(externalPaymentId);
    }

    public void markPaymentFailed(Payment payment, String reason) {
        payment.markFailed(reason);
    }

    public void markPaymentCanceled(Payment payment) {
        payment.markCanceled();
    }

    public void markPaymentRefunded(Payment payment, BigDecimal refundAmount) {
        payment.refund(refundAmount);
    }
}

