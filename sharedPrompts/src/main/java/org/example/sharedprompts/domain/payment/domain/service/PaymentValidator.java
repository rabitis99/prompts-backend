package org.example.sharedprompts.domain.payment.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.exception.PaymentDomainException;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class PaymentValidator {

    public void validateAmount(BigDecimal expectedAmount, BigDecimal actualAmount, String orderId) {
        if (expectedAmount == null || actualAmount == null) {
            log.error("결제 금액 검증 실패: 금액이 null입니다. orderId={}, expected={}, actual={}", 
                    orderId, expectedAmount, actualAmount);
            throw new PaymentDomainException(ErrorCode.PAYMENT_PROVIDER_ERROR, "amount",
                "결제 금액이 유효하지 않습니다.");
        }

        if (expectedAmount.compareTo(actualAmount) != 0) {
            log.error("결제 금액 불일치: orderId={}, expected={}, actual={}", 
                    orderId, expectedAmount, actualAmount);
            throw new PaymentDomainException(ErrorCode.PAYMENT_AMOUNT_MISMATCH, "amount",
                String.format("결제 금액이 일치하지 않습니다. 예상: %s, 실제: %s", expectedAmount, actualAmount));
        }
    }

    public void validateOrderId(String expectedOrderId, String actualOrderId) {
        if (expectedOrderId == null || actualOrderId == null) {
            log.error("주문 ID 검증 실패: 주문 ID가 null입니다. expected={}, actual={}", 
                    expectedOrderId, actualOrderId);
            throw new PaymentDomainException(ErrorCode.PAYMENT_PROVIDER_ERROR, "orderId",
                "주문 ID가 유효하지 않습니다.");
        }

        if (!expectedOrderId.equals(actualOrderId)) {
            log.error("주문 ID 불일치: expected={}, actual={}", expectedOrderId, actualOrderId);
            throw new PaymentDomainException(ErrorCode.PAYMENT_ORDER_ID_MISMATCH, "orderId",
                String.format("주문 ID가 일치하지 않습니다. 예상: %s, 실제: %s", expectedOrderId, actualOrderId));
        }
    }

    /**
     * Toss Payments의 orderId 형식을 고려하여 검증
     * Toss의 경우 "ORDER-{paymentId}-{timestamp}" 형식이므로 paymentId 부분만 추출하여 비교
     */
    public void validateOrderIdForToss(String expectedOrderId, String actualOrderId) {
        if (expectedOrderId == null || actualOrderId == null) {
            log.error("주문 ID 검증 실패: 주문 ID가 null입니다. expected={}, actual={}", 
                    expectedOrderId, actualOrderId);
            throw new PaymentDomainException(ErrorCode.PAYMENT_PROVIDER_ERROR, "orderId",
                "주문 ID가 유효하지 않습니다.");
        }

        // Toss의 경우 "ORDER-{paymentId}-{timestamp}" 형식에서 paymentId 추출
        String extractedExpectedId = extractPaymentIdFromTossOrderId(expectedOrderId);
        String extractedActualId = extractPaymentIdFromTossOrderId(actualOrderId);

        // 추출된 ID가 같거나, 원본이 같으면 통과
        if (extractedExpectedId != null && extractedActualId != null && extractedExpectedId.equals(extractedActualId)) {
            return;
        }

        // 원본이 같으면 통과 (일반적인 경우)
        if (expectedOrderId.equals(actualOrderId)) {
            return;
        }

        log.error("주문 ID 불일치: expected={}, actual={}", expectedOrderId, actualOrderId);
        throw new PaymentDomainException(ErrorCode.PAYMENT_ORDER_ID_MISMATCH, "orderId",
            String.format("주문 ID가 일치하지 않습니다. 예상: %s, 실제: %s", expectedOrderId, actualOrderId));
    }

    /**
     * Toss Payments의 orderId에서 paymentId를 추출
     * 형식: "ORDER-{paymentId}-{timestamp}" 또는 단순히 paymentId 문자열
     */
    private String extractPaymentIdFromTossOrderId(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            return null;
        }

        // "ORDER-{paymentId}-" 형식인 경우 paymentId 추출
        if (orderId.startsWith("ORDER-")) {
            String[] parts = orderId.split("-");
            if (parts.length >= 2) {
                return parts[1]; // "ORDER-2-1770360255571" -> "2"
            }
        }

        // 형식이 맞지 않으면 원본 반환
        return orderId;
    }

    public void validateCurrency(String expectedCurrency, String actualCurrency) {
        if (expectedCurrency == null || actualCurrency == null) {
            log.error("통화 코드 검증 실패: 통화 코드가 null입니다. expected={}, actual={}", 
                    expectedCurrency, actualCurrency);
            throw new PaymentDomainException(ErrorCode.PAYMENT_PROVIDER_ERROR, "currency",
                "통화 코드가 유효하지 않습니다.");
        }

        if (!expectedCurrency.equalsIgnoreCase(actualCurrency)) {
            log.error("통화 코드 불일치: expected={}, actual={}", expectedCurrency, actualCurrency);
            throw new PaymentDomainException(ErrorCode.PAYMENT_CURRENCY_MISMATCH, "currency",
                String.format("통화 코드가 일치하지 않습니다. 예상: %s, 실제: %s", expectedCurrency, actualCurrency));
        }
    }

    public void validatePaymentResult(Payment payment, PaymentResult result, BigDecimal actualAmount) {
        // Toss의 경우 orderId 형식을 고려하여 검증
        if (payment.getPaymentMethod() == PaymentMethod.TOSS) {
            validateOrderIdForToss(String.valueOf(payment.getId()), result.getOrderId());
        } else {
            validateOrderId(String.valueOf(payment.getId()), result.getOrderId());
        }
        validateAmount(actualAmount, result.getAmount(), result.getOrderId());
        validateCurrency(payment.getCurrency(), result.getCurrency());
    }

    public void validatePaymentKey(String paymentKey, PaymentMethod paymentMethod) {
        if (paymentKey == null || paymentKey.isEmpty()) {
            throw new PaymentDomainException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                "paymentKey는 필수입니다");
        }

        if (paymentMethod == PaymentMethod.TOSS) {
            if (!paymentKey.matches("^t(gen_|\\w+).*")) {
                throw new PaymentDomainException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                    "Toss paymentKey 형식이 올바르지 않습니다: " + paymentKey);
            }
            if ("Toss".equals(paymentKey) || "TOSS".equals(paymentKey)) {
                throw new PaymentDomainException(ErrorCode.INVALID_INPUT_VALUE, "paymentKey",
                    "paymentKey는 PaymentMethod가 아닌 실제 결제 세션 키여야 합니다");
            }
        }
    }

    public void validateKakaoPayPgToken(String pgToken) {
        if (pgToken == null || pgToken.isEmpty()) {
            throw new PaymentDomainException(ErrorCode.INVALID_INPUT_VALUE, "pgToken",
                "카카오페이 결제 승인을 위해서는 pgToken이 필수입니다");
        }
    }
}

