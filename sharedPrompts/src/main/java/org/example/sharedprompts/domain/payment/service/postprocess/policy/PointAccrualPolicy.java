package org.example.sharedprompts.domain.payment.service.postprocess.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 포인트 적립 기준 금액 결정 정책
 *
 * <p><strong>현재 정책:</strong> 실제 결제 금액(actualPaymentAmount) 기준
 *
 * <p><strong>정책 근거:</strong>
 * <ul>
 *   <li>포인트로 포인트를 적립하는 순환 구조 방지</li>
 *   <li>실제 현금 결제에 대해서만 리워드 제공</li>
 * </ul>
 *
 * <p><strong>예시:</strong> 1000원 주문 - 100원 포인트 사용 = 900원 실제 결제
 * <ul>
 *   <li>포인트 적립 기준: 900원</li>
 *   <li>포인트 적립: 900원 × 1% = 9포인트</li>
 * </ul>
 *
 * <p><strong>대안 정책 (업계 관례):</strong>
 * 원래 주문 금액(originalAmount) 기준으로 변경 시, 포인트 사용과 무관하게
 * 주문 금액 전체에 대해 적립됩니다. 이 경우 determineBasisAmount 메서드를
 * 수정하거나, 새로운 구현체를 생성하세요.
 *
 * @see RewardBasisPolicy
 */
@Component
public class PointAccrualPolicy implements RewardBasisPolicy {

    private static final String POLICY_NAME = "ACTUAL_PAYMENT_AMOUNT";
    private static final String POLICY_DESCRIPTION = "포인트 적립은 실제 결제 금액(포인트 차감 후) 기준으로 계산됩니다.";

    /**
     * 포인트 적립 기준 금액을 결정합니다.
     *
     * <p>현재 정책: 실제 결제 금액(actualPaymentAmount) 반환
     *
     * @param originalAmount 포인트 차감 전 원래 주문 금액
     * @param actualPaymentAmount 포인트 차감 후 실제 결제 금액
     * @param usedPointAmount 사용된 포인트 금액
     * @return 실제 결제 금액 (actualPaymentAmount)
     */
    @Override
    public BigDecimal determineBasisAmount(BigDecimal originalAmount, BigDecimal actualPaymentAmount, BigDecimal usedPointAmount) {
        if (actualPaymentAmount == null) {
            return BigDecimal.ZERO;
        }
        return actualPaymentAmount;
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
