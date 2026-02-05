package org.example.sharedprompts.domain.payment.provider.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.CancelResult;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.model.RefundResult;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.provider.paypal.client.PaypalCaptureApiClient;
import org.example.sharedprompts.domain.payment.provider.paypal.client.PaypalCreateOrderApiClient;
import org.example.sharedprompts.domain.payment.provider.paypal.client.PaypalRefundApiClient;
import org.example.sharedprompts.domain.payment.provider.paypal.client.PaypalStatusApiClient;
import org.example.sharedprompts.domain.payment.provider.paypal.client.PaypalVoidApiClient;
import org.example.sharedprompts.domain.payment.provider.paypal.mapper.PayPalStatusMapper;
import org.example.sharedprompts.domain.payment.provider.paypal.policy.PayPalAmountPolicy;
import org.example.sharedprompts.domain.payment.provider.paypal.policy.PayPalRefundPolicy;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * PayPal Payment Provider 구현체
 *
 * <p>단일 책임: PaymentProvider 인터페이스 구현 및 모듈 조합
 * - client: 외부 API 호출 (분리된 클라이언트들)
 * - mapper: 상태 매핑
 * - policy: 금액 검증, 환불 정책
 * - webhook: 서명 검증과 payload 파싱
 *
 * <p>Null 안전성: 모든 public API는 Null 반환 금지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayPalPaymentProvider implements PaymentProvider {

    private final PaypalCreateOrderApiClient paypalCreateOrderApiClient;
    private final PaypalCaptureApiClient paypalCaptureApiClient;
    private final PaypalStatusApiClient paypalStatusApiClient;
    private final PaypalVoidApiClient paypalVoidApiClient;
    private final PaypalRefundApiClient paypalRefundApiClient;
    private final PayPalAmountPolicy amountPolicy;
    private final PayPalRefundPolicy refundPolicy;
    private final PayPalStatusMapper statusMapper;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.PAYPAL;
    }

    @Override
    public boolean requiresPreparation() {
        return true; // PayPal은 결제 승인 전 주문 생성 단계가 필요
    }

    @Override
    public PrepareResult preparePayment(
            String orderId,
            BigDecimal amount,
            String currency,
            String itemName,
            String userId,
            String idempotencyKey
    ) {
        validateRequired(orderId, "orderId");
        validateRequired(amount, "amount");
        validateRequired(currency, "currency");

        try {
            BigDecimal validatedAmount = amountPolicy.validate(amount);
            var response = paypalCreateOrderApiClient.createOrder(orderId, validatedAmount, currency, itemName, idempotencyKey);

            log.info("PayPal 주문 생성 성공: orderId={}, paypalOrderId={}", 
                    orderId, SensitiveDataMasker.maskPaymentKey(response.orderId()));
            return PrepareResult.success(
                    response.orderId(),
                    response.approveUrl(),
                    response.metadata()
            );
        } catch (Exception e) {
            log.error("PayPal 주문 생성 실패: orderId={}, error={}", 
                    orderId, SensitiveDataMasker.maskSensitiveData(e.getMessage()), e);
            throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "PayPal 주문 생성 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentResult confirmPayment(
            String paymentKey, // PayPal에서는 orderId
            String orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            String userId, // PayPal에서는 사용하지 않음
            java.util.Map<String, String> additionalParams // PayPal에서는 사용하지 않음
    ) {
        validateRequired(paymentKey, "paymentKey (orderId)");
        validateRequired(orderId, "orderId");
        validateRequired(amount, "amount");
        validateRequired(currency, "currency");

        try {
            amountPolicy.validate(amount); // 금액 검증만 수행
            var response = paypalCaptureApiClient.capture(paymentKey, idempotencyKey);
            PaymentStatus status = statusMapper.map(response.status());

            log.info("PayPal 결제 승인 성공: orderId={}, status={}", 
                    SensitiveDataMasker.maskPaymentKey(paymentKey), status);
            return PaymentResult.builder()
                    .externalPaymentId(paymentKey)
                    .status(status)
                    .amount(response.amount())
                    .currency(response.currency())
                    .orderId(orderId)
                    .approvedAt(LocalDateTime.ofInstant(response.approvedAt(), ZoneId.systemDefault()))
                    .metadata(response.metadata())
                    .build();
        } catch (Exception e) {
            log.error("PayPal 결제 승인 실패: orderId={}, error={}", 
                    SensitiveDataMasker.maskPaymentKey(paymentKey), 
                    SensitiveDataMasker.maskSensitiveData(e.getMessage()), e);
            return PaymentResult.builder()
                    .externalPaymentId(paymentKey)
                    .status(PaymentStatus.FAILED)
                    .amount(amount)
                    .currency(currency)
                    .orderId(orderId)
                    .failureReason("PayPal API 호출 실패: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResult getPaymentStatus(String externalPaymentId) {
        validateRequired(externalPaymentId, "externalPaymentId");

        try {
            var response = paypalStatusApiClient.getOrderStatus(externalPaymentId);
            PaymentStatus status = statusMapper.map(response.status());

            log.debug("PayPal 결제 상태 조회 성공: orderId={}, status={}", 
                    SensitiveDataMasker.maskPaymentKey(externalPaymentId), status);
            return PaymentResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(status)
                    .amount(response.amount())
                    .currency(response.currency())
                    .orderId(externalPaymentId)
                    .approvedAt(LocalDateTime.ofInstant(response.approvedAt(), ZoneId.systemDefault()))
                    .metadata(response.metadata())
                    .build();
        } catch (Exception e) {
            log.error("PayPal 결제 상태 조회 실패: orderId={}, error={}", 
                    SensitiveDataMasker.maskPaymentKey(externalPaymentId), 
                    SensitiveDataMasker.maskSensitiveData(e.getMessage()), e);
            return PaymentResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(PaymentStatus.FAILED)
                    .failureReason("PayPal 상태 조회 실패: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 결제 취소
     */
    @Override
    public CancelResult cancelPayment(String externalPaymentId, String reason, String idempotencyKey) {
        validateRequired(externalPaymentId, "externalPaymentId");
        validateRequired(reason, "reason");

        try {
            // 먼저 주문 상태를 조회하여 취소 방법 결정
            var orderDetails = paypalStatusApiClient.getOrderDetails(externalPaymentId);
            String orderStatus = orderDetails.status();

            // CREATED 또는 APPROVED 상태 (미인증/미캡처): API 호출 없이 시스템에서 취소 처리
            if ("CREATED".equals(orderStatus) || "APPROVED".equals(orderStatus)) {
                log.info("PayPal 주문 취소 (미캡처 상태): externalPaymentId={}, status={}", 
                        SensitiveDataMasker.maskPaymentKey(externalPaymentId), orderStatus);

                return CancelResult.builder()
                        .externalPaymentId(externalPaymentId)
                        .status(PaymentStatus.CANCELED)
                        .canceledAt(LocalDateTime.now())
                        .reason(reason)
                        .metadata(orderDetails.metadata())
                        .build();
            }

            // Authorization이 존재하는 경우: void 처리
            if (orderDetails.authorizationId() != null) {
                var voidResponse = paypalVoidApiClient.voidAuthorization(orderDetails.authorizationId(), idempotencyKey);

                log.info("PayPal Authorization void 성공: externalPaymentId={}", 
                        SensitiveDataMasker.maskPaymentKey(externalPaymentId));
                return CancelResult.builder()
                        .externalPaymentId(externalPaymentId)
                        .status(PaymentStatus.CANCELED)
                        .canceledAt(LocalDateTime.ofInstant(voidResponse.canceledAt(), ZoneId.systemDefault()))
                        .reason(reason)
                        .metadata(voidResponse.metadata())
                        .build();
            }

            // Capture가 존재하는 경우: 이미 캡처된 결제는 취소가 아닌 환불 처리 필요
            if (orderDetails.captureId() != null) {
                throw new ApiException(ErrorCode.PAYMENT_CANCEL_FAILED,
                        "PayPal 결제 취소 실패: 이미 캡처된 결제는 취소할 수 없습니다. 환불(refund)을 사용해주세요. " +
                        "externalPaymentId=" + externalPaymentId + ", captureId=" + orderDetails.captureId());
            }

            // 그 외의 경우: 주문 상태만 반환
            log.warn("PayPal 주문 취소 (상태 확인): externalPaymentId={}, status={}, " +
                    "authorizationId={}, captureId={}",
                    SensitiveDataMasker.maskPaymentKey(externalPaymentId), orderStatus, 
                    orderDetails.authorizationId() != null ? SensitiveDataMasker.maskPaymentKey(orderDetails.authorizationId()) : null,
                    orderDetails.captureId() != null ? SensitiveDataMasker.maskPaymentKey(orderDetails.captureId()) : null);

            return CancelResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(PaymentStatus.CANCELED)
                    .canceledAt(LocalDateTime.now())
                    .reason(reason)
                    .metadata(orderDetails.metadata())
                    .build();

        } catch (Exception e) {
            log.error("PayPal 결제 취소 실패: externalPaymentId={}, error={}", 
                    SensitiveDataMasker.maskPaymentKey(externalPaymentId), 
                    SensitiveDataMasker.maskSensitiveData(e.getMessage()), e);
            throw new ApiException(ErrorCode.PAYMENT_CANCEL_FAILED, "PayPal 결제 취소 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public RefundResult refundPayment(String externalPaymentId, BigDecimal amount, String reason, String idempotencyKey) {
        validateRequired(externalPaymentId, "externalPaymentId");
        validateRequired(amount, "amount");
        validateRequired(reason, "reason");

        try {
            BigDecimal validatedAmount = amountPolicy.validate(amount);

            // 주문에서 캡처 정보 및 통화 코드 조회 (단일 API 호출로 최적화)
            var orderDetails = paypalStatusApiClient.getOrderDetails(externalPaymentId);
            String captureId = orderDetails.captureId();
            if (captureId == null || captureId.isEmpty()) {
                throw new ApiException(ErrorCode.PAYMENT_REFUND_FAILED,
                        "PayPal 환불 실패: 캡처된 결제가 없습니다. externalPaymentId=" + externalPaymentId);
            }
            String currency = orderDetails.currency();

            var response = paypalRefundApiClient.refund(captureId, validatedAmount, currency, reason, idempotencyKey);
            PaymentStatus refundStatus = refundPolicy.determineStatus(response.refundedAmount(), validatedAmount, response.status());

            log.info("PayPal 결제 환불 성공: externalPaymentId={}, refundedAmount={}", 
                    SensitiveDataMasker.maskPaymentKey(externalPaymentId), response.refundedAmount());
            return RefundResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .status(refundStatus)
                    .refundedAmount(response.refundedAmount())
                    .refundedAt(LocalDateTime.ofInstant(response.refundedAt(), ZoneId.systemDefault()))
                    .reason(reason)
                    .metadata(response.metadata())
                    .build();
        } catch (Exception e) {
            log.error("PayPal 결제 환불 실패: externalPaymentId={}, amount={}, error={}", 
                    SensitiveDataMasker.maskPaymentKey(externalPaymentId), amount, 
                    SensitiveDataMasker.maskSensitiveData(e.getMessage()), e);
            throw new ApiException(ErrorCode.PAYMENT_REFUND_FAILED, "PayPal 결제 환불 실패: " + e.getMessage(), e);
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