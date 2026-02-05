package org.example.sharedprompts.domain.payment.application.command.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.toss.exception.DuplicateOrderIdException;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.domain.service.PaymentValidator;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class DuplicateOrderIdHandler {

    private final PaymentProviderFactory providerFactory;
    private final PaymentValidator paymentValidator;
    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentResultProcessor resultProcessor;

    public Payment handleDuplicateOrderIdException(
            Long paymentId,
            DuplicateOrderIdException e,
            BigDecimal actualAmount
    ) {
        log.warn("TossPay 중복 주문번호 오류 발생: paymentId={}, paymentKey={}, orderId={}. " +
                "결제 상태를 확인합니다.", paymentId, e.getPaymentKey(), e.getOrderId());
        
        Payment freshPayment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        
        if (freshPayment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment가 이미 SUCCESS 상태입니다 (webhook으로 처리됨): paymentId={}", paymentId);
            return freshPayment;
        }
        
        return checkPaymentStatusAndUpdate(freshPayment, e, actualAmount);
    }

    private Payment checkPaymentStatusAndUpdate(
            Payment payment,
            DuplicateOrderIdException e,
            BigDecimal actualAmount
    ) {
        try {
            PaymentProvider statusProvider = providerFactory.getProvider(payment.getPaymentMethod());
            PaymentResult statusResult = statusProvider.getPaymentStatus(payment.getExternalPaymentId());
            
            if (statusResult.isSuccess()) {
                log.info("TossPayments에서 결제가 이미 확인되었습니다: paymentId={}, externalPaymentId={}", 
                        payment.getId(), payment.getExternalPaymentId());
                
                paymentValidator.validatePaymentResult(payment, statusResult, actualAmount);
                resultProcessor.markPaymentSuccess(payment, statusResult.getExternalPaymentId());
            } else {
                log.warn("TossPayments에서 결제가 실패 상태입니다: paymentId={}, status={}", 
                        payment.getId(), statusResult.getStatus());
                resultProcessor.markPaymentFailed(payment, "중복 주문번호 오류: " + e.getMessage());
            }
            
            return paymentJpaAdapter.save(payment);
        } catch (Exception statusCheckException) {
            log.error("TossPayments 결제 상태 조회 실패: paymentId={}, error={}", 
                    payment.getId(), statusCheckException.getMessage(), statusCheckException);
            resultProcessor.markPaymentFailed(payment, "중복 주문번호 오류 및 상태 조회 실패: " + e.getMessage());
            return paymentJpaAdapter.save(payment);
        }
    }
}

