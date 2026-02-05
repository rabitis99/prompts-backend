package org.example.sharedprompts.domain.payment.infrastructure.idempotency;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 멱등성 키 생성 서비스 구현체
 *
 * <p><strong>현재 전략:</strong>
 * 결정론적 키 생성 (동일한 Payment에 대해 항상 같은 키 반환)
 *
 * <p><strong>향후 개선 가능성:</strong>
 * - UUID 기반 전략
 * - 해시 기반 전략
 * - 결제사별 특화 전략
 */
@Slf4j
@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    @Override
    public String generateForPayment(Payment payment) {
        // 기존에 저장된 idempotencyKey가 있으면 재사용
        if (payment.getIdempotencyKey() != null) {
            return payment.getIdempotencyKey();
        }
        // 없으면 paymentMethod:paymentId 형식으로 생성
        return String.format("%s:%s",
                payment.getPaymentMethod().name(),
                payment.getId());
    }

    @Override
    public String generateForCancel(Payment payment) {
        return String.format("%s:%s:%s",
                payment.getPaymentMethod().name(),
                payment.getId(),
                "cancel");
    }

    @Override
    public String generateForRefund(Payment payment) {
        // refundedAmount를 포함하여 각 부분 환불 요청을 구분
        // refundedAmount가 같은 상태에서 재시도하면 같은 키가 생성되어 멱등성 보장
        // null 방어: DB에서 로드 시 null일 수 있으므로 기본값 사용
        BigDecimal refundedAmount = payment.getRefundedAmount() != null
                ? payment.getRefundedAmount()
                : BigDecimal.ZERO;
        return String.format("%s:%s:refund:%s",
                payment.getPaymentMethod().name(),
                payment.getId(),
                refundedAmount.stripTrailingZeros().toPlainString());
    }
}

