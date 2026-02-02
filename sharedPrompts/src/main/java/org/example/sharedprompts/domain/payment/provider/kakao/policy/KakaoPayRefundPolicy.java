package org.example.sharedprompts.domain.payment.provider.kakao.policy;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class KakaoPayRefundPolicy {

    /**
     * KakaoPay 환불 상태 결정
     *
     * @param refundedAmount 실제 환불된 금액
     * @param originalAmount 원 결제 금액
     */
    public PaymentStatus determineStatus(
            BigDecimal refundedAmount,
            BigDecimal originalAmount
    ) {
        if (originalAmount != null && refundedAmount.compareTo(originalAmount) >= 0) {
            return PaymentStatus.REFUNDED;
        }
        return PaymentStatus.PARTIALLY_REFUNDED;
    }

    /**
     * KakaoPay는 cancel API에서 null 금액을 허용하지 않음
     */
    public long resolveCancelAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("KakaoPay 환불 금액은 null일 수 없습니다.");
        }
        return amount.longValueExact();
    }

    /**
     * 현재 KakaoPay 정책상 면세 금액은 별도 정책 없으면 0 처리
     */
    public long resolveTaxFreeAmount(BigDecimal taxFreeAmount) {
        return taxFreeAmount != null
                ? taxFreeAmount.longValueExact()
                : 0L;
    }
}
