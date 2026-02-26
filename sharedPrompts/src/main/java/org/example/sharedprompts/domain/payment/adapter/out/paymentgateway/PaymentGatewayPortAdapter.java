package org.example.sharedprompts.domain.payment.adapter.out.paymentgateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.dto.response.CancelResult;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.application.dto.response.RefundResult;
import org.example.sharedprompts.domain.payment.application.port.out.paymentgateway.PaymentGatewayPort;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProviderFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 결제 게이트웨이 포트 어댑터
 * PaymentGatewayPort를 구현하여 외부 PG 연동을 추상화합니다.
 *
 * 기존의 PaymentProvider들을 활용하여 결제 게이트웨이 기능을 제공합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayPortAdapter implements PaymentGatewayPort {

    private final PaymentProviderFactory providerFactory;

    @Override
    public PaymentGatewayResult preparePayment(Payment payment) {
        log.info("결제 준비 시작: paymentId={}, method={}", payment.getId(), payment.getPaymentMethod());

        try {
            PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());
            PaymentProvider.PrepareResult result = provider.preparePayment(
                    payment.getId().toString(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    "Payment Item", // itemName (구체적인 값은 필요시 추가)
                    payment.getUser().getId().toString(),
                    payment.getIdempotencyKey()
            );

            // 결과 변환
            String externalPaymentId = result.tid();

            log.info("결제 준비 완료: paymentId={}, externalId={}", payment.getId(), externalPaymentId);
            return PaymentGatewayResult.success(externalPaymentId, null);

        } catch (Exception e) {
            log.error("결제 준비 중 오류: paymentId={}, error={}", payment.getId(), e.getMessage(), e);
            return PaymentGatewayResult.failure("결제 준비 중 오류: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentGatewayResult confirmPayment(Payment payment, String approvalToken) {
        log.info("결제 승인 시작: paymentId={}, method={}", payment.getId(), payment.getPaymentMethod());

        try {
            PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());
            PaymentResult result = provider.confirmPayment(
                    approvalToken,
                    payment.getId().toString(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    payment.getIdempotencyKey(),
                    payment.getUser().getId().toString(),
                    new java.util.HashMap<>()
            );

            String externalPaymentId = result.getExternalPaymentId();

            log.info("결제 승인 완료: paymentId={}, externalId={}", payment.getId(), externalPaymentId);
            return PaymentGatewayResult.success(externalPaymentId, null);

        } catch (Exception e) {
            log.error("결제 승인 중 오류: paymentId={}, error={}", payment.getId(), e.getMessage(), e);
            return PaymentGatewayResult.failure("결제 승인 중 오류: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentGatewayResult queryPaymentStatus(String externalPaymentId) {
        log.info("결제 상태 조회 시작: externalId={}", externalPaymentId);

        try {
            // NOTE: 이 경우 제공자를 특정하기 어려움. 별도의 결제 조회 기능 필요
            // 현재는 예외 처리
            throw new UnsupportedOperationException(
                    "externalPaymentId만으로는 제공자를 특정할 수 없습니다. " +
                    "Payment 엔티티를 통해 조회하세요."
            );

        } catch (Exception e) {
            log.error("결제 상태 조회 중 오류: {}", e.getMessage(), e);
            return PaymentGatewayResult.failure("결제 상태 조회 중 오류: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentGatewayResult cancelPayment(Payment payment) {
        log.info("결제 취소 시작: paymentId={}, method={}", payment.getId(), payment.getPaymentMethod());

        try {
            PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());
            CancelResult result = provider.cancelPayment(
                    payment.getExternalPaymentId(),
                    "User requested cancel",
                    payment.getIdempotencyKey()
            );

            log.info("결제 취소 완료: paymentId={}", payment.getId());
            return PaymentGatewayResult.success(payment.getExternalPaymentId(), null);

        } catch (Exception e) {
            log.error("결제 취소 중 오류: paymentId={}, error={}", payment.getId(), e.getMessage(), e);
            return PaymentGatewayResult.failure("결제 취소 중 오류: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentGatewayResult refundPayment(Payment payment, BigDecimal refundAmount) {
        log.info("환불 시작: paymentId={}, amount={}, method={}",
                payment.getId(), refundAmount, payment.getPaymentMethod());

        try {
            PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());
            RefundResult result = provider.refundPayment(
                    payment.getExternalPaymentId(),
                    refundAmount,
                    "User requested refund",
                    payment.getIdempotencyKey()
            );

            log.info("환불 완료: paymentId={}, amount={}", payment.getId(), refundAmount);
            return PaymentGatewayResult.success(payment.getExternalPaymentId(), null);

        } catch (Exception e) {
            log.error("환불 중 오류: paymentId={}, error={}", payment.getId(), e.getMessage(), e);
            return PaymentGatewayResult.failure("환불 중 오류: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentGatewayResult queryTransactionDetails(String externalPaymentId) {
        log.info("거래 상세 조회 시작: externalId={}", externalPaymentId);

        try {
            // NOTE: externalPaymentId만으로는 제공자를 알 수 없음
            throw new UnsupportedOperationException(
                    "externalPaymentId만으로는 제공자를 특정할 수 없습니다."
            );

        } catch (Exception e) {
            log.error("거래 상세 조회 중 오류: {}", e.getMessage(), e);
            return PaymentGatewayResult.failure("거래 상세 조회 중 오류: " + e.getMessage(), e);
        }
    }
}
