package org.example.sharedprompts.domain.payment.service.postprocess.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 캐시백 적립 기준 금액 결정 정책
 *
 * <p><strong>현재 정책:</strong> 원래 주문 금액(originalAmount) 기준
 *
 * <p><strong>정책 근거:</strong>
 * <ul>
 *   <li>주문 금액 전체에 대한 캐시백 혜택 제공</li>
 *   <li>고객 만족도 및 재방문율 향상</li>
 * </ul>
 *
 * <p><strong>예시:</strong> 1000원 주문 - 100원 포인트 사용 = 900원 실제 결제
 * <ul>
 *   <li>캐시백 적립 기준: 1000원</li>
 *   <li>캐시백 적립: 1000원 × 1% = 10원 캐시백</li>
 * </ul>
 *
 * <p><strong>주의사항:</strong>
 * 포인트 적립 정책(PointAccrualPolicy)과 기준 금액이 다릅니다.
 * 이는 의도된 정책 차이이며, 변경 시 비즈니스 검토가 필요합니다.
 *
 * <p><strong>대안 정책 (업계 관례):</strong>
 * 실제 결제 금액(actualPaymentAmount) 기준으로 변경 시, 실제 현금 결제에
 * 대해서만 캐시백이 제공됩니다. 이 경우 determineBasisAmount 메서드를
 * 수정하거나, 새로운 구현체를 생성하세요.
 *
 * @see RewardBasisPolicy
 */
@Component
public class CashbackAccrualPolicy implements RewardBasisPolicy {

    private static final String POLICY_NAME = "ORIGINAL_AMOUNT";
    private static final String POLICY_DESCRIPTION = "캐시백 적립은 원래 주문 금액(포인트 차감 전) 기준으로 계산됩니다.";

    /**
     * 캐시백 적립 기준 금액을 결정합니다.
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
