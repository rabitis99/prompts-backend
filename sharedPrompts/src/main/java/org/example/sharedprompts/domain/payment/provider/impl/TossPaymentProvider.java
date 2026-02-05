package org.example.sharedprompts.domain.payment.provider.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.CancelResult;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.model.RefundResult;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.provider.toss.client.TossCancelApiClient;
import org.example.sharedprompts.domain.payment.provider.toss.client.TossConfirmApiClient;
import org.example.sharedprompts.domain.payment.provider.toss.client.TossStatusApiClient;
import org.example.sharedprompts.domain.payment.provider.toss.mapper.TossPayStatusMapper;
import org.example.sharedprompts.domain.payment.provider.toss.policy.TossPayAmountPolicy;
import org.example.sharedprompts.domain.payment.provider.toss.policy.TossPayRefundPolicy;
import org.example.sharedprompts.domain.payment.provider.toss.exception.DuplicateOrderIdException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * TossPay Payment Provider 구현체
 *
 * <p>단일 책임: PaymentProvider 인터페이스 구현 및 모듈 조합
 * - client: 외부 API 호출 (분리된 클라이언트들)
 * - mapper: 상태 매핑
 * - policy: 금액 검증/변환, 환불 정책
 * - webhook: 서명 검증과 payload 파싱
 *
 * <p>Null 안전성: 모든 public API는 Null 반환 금지
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class TossPaymentProvider implements PaymentProvider {

    private final TossConfirmApiClient tossConfirmApiClient;
    private final TossStatusApiClient tossStatusApiClient;
    private final TossCancelApiClient tossCancelApiClient;
    private final TossPayAmountPolicy amountPolicy;
    private final TossPayRefundPolicy refundPolicy;
    private final TossPayStatusMapper statusMapper;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.TOSS;
    }

    @Override
    public boolean requiresPreparation() {
        return false; // 클라이언트에서 결제 위젯이 paymentKey를 발급
    }

    @Override
    public PaymentResult confirmPayment(
            String paymentKey,
            String orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            String userId, // TossPay에서는 사용하지 않음
            java.util.Map<String, String> additionalParams // TossPay에서는 사용하지 않음
    ) {
        validateRequired(paymentKey, "paymentKey");
        validateRequired(orderId, "orderId");
        validateRequired(amount, "amount");
        validateRequired(currency, "currency");

        try {
            long tossAmount = amountPolicy.toTossAmount(amount);
            var response = tossConfirmApiClient.confirm(paymentKey, orderId, tossAmount);
            PaymentStatus status = statusMapper.map(response.status());

            log.info("TossPay 결제 승인 성공: paymentKey={}, orderId={}, status={}", paymentKey, orderId, status);
            return PaymentResult.builder()
                    .externalPaymentId(response.paymentKey())
                    .status(status)
                    .amount(response.totalAmount())
                    .currency(response.currency())
                    .orderId(response.orderId())
                    .approvedAt(response.approvedAt())
                    .metadata(response.metadata())
                    .build();
        } catch (DuplicateOrderIdException e) {
            // S021 오류는 상위로 전파하여 특별 처리
            log.warn("TossPay 중복 주문번호 오류: paymentKey={}, orderId={}", paymentKey, orderId);
            throw e;
        } catch (Exception e) {
            log.error("TossPay 결제 승인 실패: paymentKey={}, orderId={}, error={}", paymentKey, orderId, e.getMessage(), e);
            return PaymentResult.builder()
                    .externalPaymentId(paymentKey)
                    .status(PaymentStatus.FAILED)
                    .amount(amount)
                    .currency(currency)
                    .orderId(orderId)
                    .failureReason("TossPay API 호출 실패: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResult getPaymentStatus(String externalPaymentId) {
        validateRequired(externalPaymentId, "externalPaymentId");

        try {
            var response = tossStatusApiClient.status(externalPaymentId);
            PaymentStatus status = statusMapper.map(response.status());

            log.debug("TossPay 결제 상태 조회 성공: paymentKey={}, status={}", externalPaymentId, status);
            return PaymentResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(status)
                    .amount(response.totalAmount())
                    .currency(response.currency())
                    .orderId(response.orderId())
                    .approvedAt(response.approvedAt())
                    .metadata(response.metadata())
                    .build();
        } catch (Exception e) {
            log.error("TossPay 결제 상태 조회 실패: paymentKey={}, error={}", externalPaymentId, e.getMessage(), e);
            return PaymentResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(PaymentStatus.FAILED)
                    .failureReason("TossPay 상태 조회 실패: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public CancelResult cancelPayment(String externalPaymentId, String reason, String idempotencyKey) {
        validateRequired(externalPaymentId, "externalPaymentId");
        validateRequired(reason, "reason");

        try {
            var response = tossCancelApiClient.cancel(externalPaymentId, reason);

            log.info("TossPay 결제 취소 성공: paymentKey={}", externalPaymentId);
            return CancelResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(PaymentStatus.CANCELED)
                    .canceledAt(response.canceledAt())
                    .reason(reason)
                    .metadata(response.metadata())
                    .build();
        } catch (Exception e) {
            log.error("TossPay 결제 취소 실패: paymentKey={}, error={}", externalPaymentId, e.getMessage(), e);
            throw new RuntimeException("TossPay 결제 취소 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public RefundResult refundPayment(String externalPaymentId, BigDecimal amount, String reason, String idempotencyKey) {
        validateRequired(externalPaymentId, "externalPaymentId");
        validateRequired(amount, "amount");
        validateRequired(reason, "reason");

        try {
            long tossAmount = amountPolicy.toTossAmount(amount);
            var response = tossCancelApiClient.refund(externalPaymentId, tossAmount, reason);

            BigDecimal refundedAmount = amountPolicy.fromTossAmount(response.refundedAmount());
            PaymentStatus refundStatus = refundPolicy.determineStatus(refundedAmount, amount);

            log.info("TossPay 결제 환불 성공: paymentKey={}, refundedAmount={}", externalPaymentId, refundedAmount);
            return RefundResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(refundStatus)
                    .refundedAmount(refundedAmount)
                    .refundedAt(response.refundedAt())
                    .reason(reason)
                    .metadata(response.metadata())
                    .build();
        } catch (Exception e) {
            log.error("TossPay 결제 환불 실패: paymentKey={}, amount={}, error={}", externalPaymentId, amount, e.getMessage(), e);
            throw new RuntimeException("TossPay 결제 환불 실패: " + e.getMessage(), e);
        }
    }


    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }

    private void validateRequired(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + "은(는) 0보다 커야 합니다: " + value);
        }
    }

}
