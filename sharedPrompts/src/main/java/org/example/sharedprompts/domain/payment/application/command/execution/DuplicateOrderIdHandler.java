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
        log.warn("TossPay 중복 주문번호 오류 발생: paymentId={}, paymentKey={}, orderId={}. 결제 상태를 확인합니다.",
                paymentId, e.getPaymentKey(), e.getOrderId());

        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment가 이미 SUCCESS 상태입니다 (webhook으로 처리됨): paymentId={}", paymentId);
            return payment;
        }

        return checkPaymentStatusAndUpdate(payment, e, actualAmount);
    }

    private Payment checkPaymentStatusAndUpdate(
            Payment payment,
            DuplicateOrderIdException e,
            BigDecimal actualAmount
    ) {
        // externalPaymentId 확보: Payment -> DuplicateOrderIdException
        String externalPaymentId = firstNonEmpty(payment.getExternalPaymentId(), e.getPaymentKey());

        if (externalPaymentId == null) {
            // 외부 결제 ID가 없으면 상태 조회 불가 -> 명시적 예외
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "외부 결제 ID가 없어 상태 조회 불가");
        }

        // 확보된 ID를 Payment에 반영
        payment.updateExternalPaymentId(externalPaymentId);

        try {
            PaymentProvider statusProvider = providerFactory.getProvider(payment.getPaymentMethod());
            PaymentResult statusResult = statusProvider.getPaymentStatus(externalPaymentId);

            if (statusResult.isSuccess()) {
                log.info("TossPayments에서 결제가 이미 확인되었습니다: paymentId={}, externalPaymentId={}",
                        payment.getId(), externalPaymentId);
                paymentValidator.validatePaymentResult(payment, statusResult, actualAmount);
                resultProcessor.markPaymentSuccess(payment, statusResult.getExternalPaymentId());
            } else {
                log.warn("TossPayments에서 결제가 실패 상태입니다: paymentId={}, status={}",
                        payment.getId(), statusResult.getStatus());
                resultProcessor.markPaymentFailed(payment, "중복 주문번호 오류: " + e.getMessage());
            }
        } catch (Exception ex) {
            log.error("TossPayments 결제 상태 조회 실패: paymentId={}, error={}",
                    payment.getId(), ex.getMessage(), ex);
            resultProcessor.markPaymentFailed(payment, "중복 주문번호 오류 및 상태 조회 실패: " + e.getMessage());
        }

        return paymentJpaAdapter.save(payment);
    }

    /** 여러 문자열 중 가장 먼저 나온 null/빈 문자열이 아닌 값을 반환, 없으면 null */
    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) return value;
        }
        return null;
    }
}
