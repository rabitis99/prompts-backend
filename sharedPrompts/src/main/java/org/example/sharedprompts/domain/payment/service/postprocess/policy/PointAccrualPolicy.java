package org.example.sharedprompts.domain.payment.service.postprocess.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 포인트 적립 기준 금액 결정 정책
 *
 * <p><strong>현재 정책:</strong> 원래 주문 금액(originalAmount) 기준
 *
 * <p><strong>정책 근거:</strong>
 * <ul>
 *   <li>포인트로 포인트를 적립하는 순환 구조 방지 (실제 결제 금액 기준 시 포인트 순환 발생)</li>
 *   <li>업계 관례: 주문 금액 전체에 대해 포인트 적립 (포인트 사용과 무관)</li>
 *   <li>사용자 경험: 포인트 사용 시에도 주문 금액 기준으로 적립하여 사용자 만족도 향상</li>
 * </ul>
 *
 * <p><strong>예시:</strong> 1000원 주문 - 100원 포인트 사용 = 900원 실제 결제
 * <ul>
 *   <li>포인트 적립 기준: 1000원 (originalAmount)</li>
 *   <li>포인트 적립: 1000원 × 1% = 10포인트</li>
 * </ul>
 *
 * <p><strong>참고:</strong>
 * 캐시백 적립은 실제 결제 금액(actualPaymentAmount) 기준으로 계산됩니다.
 * 포인트와 캐시백의 적립 기준이 다르므로 비즈니스 정책을 명확히 이해해야 합니다.
 *
 * @see RewardBasisPolicy
 * @see CashbackAccrualPolicy
 */
@Component
public class PointAccrualPolicy implements RewardBasisPolicy {

    private static final String POLICY_NAME = "ORIGINAL_ORDER_AMOUNT";
    private static final String POLICY_DESCRIPTION = "포인트 적립은 원래 주문 금액(포인트 차감 전) 기준으로 계산됩니다.";

    /**
     * 포인트 적립 기준 금액을 결정합니다.
     *
     * <p>현재 정책: 원래 주문 금액(originalAmount) 반환
     *
     * @param originalAmount 포인트 차감 전 원래 주문 금액
     * @param actualPaymentAmount 포인트 차감 후 실제 결제 금액
     * @param usedPointAmount 사용된 포인트 금액
     * @return 원래 주문 금액 (originalAmount)
     */
    @Override
    public BigDecimal determineBasisAmount(BigDecimal originalAmount, BigDecimal actualPaymentAmount, BigDecimal usedPointAmount) {
        if (originalAmount == null) {
            return BigDecimal.ZERO;
        }
        return originalAmount;
    }

    @Override
    public String getPolicyName() {
        return POLICY_NAME;
    }

    @Override
    public String getPolicyDescription() {
        return POLICY_DESCRIPTION;
    }
}
