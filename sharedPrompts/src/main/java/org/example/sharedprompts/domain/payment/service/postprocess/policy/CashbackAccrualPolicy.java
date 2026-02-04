package org.example.sharedprompts.domain.payment.service.postprocess.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 캐시백 적립 기준 금액 결정 정책
 *
 * <p><strong>현재 정책:</strong> 실제 결제 금액(actualPaymentAmount) 기준
 *
 * <p><strong>정책 근거 (2024-02-02 의사결정):</strong>
 * <ul>
 *   <li>실제 현금 결제에 대해서만 캐시백 제공 (업계 관례)</li>
 *   <li>포인트로 캐시백을 적립하는 순환 구조 방지</li>
 *   <li>포인트 적립 정책(PointAccrualPolicy)과 일관성 유지</li>
 * </ul>
 *
 * <p><strong>예시:</strong> 1000원 주문 - 100원 포인트 사용 = 900원 실제 결제
 * <ul>
 *   <li>캐시백 적립 기준: 900원</li>
 *   <li>캐시백 적립: 900원 × 1% = 9원 캐시백</li>
 * </ul>
 *
 * <p><strong>변경 이력:</strong>
 * 기존에는 originalAmount 기준이었으나, 포인트 사용 시 과다 캐시백 지급 문제와
 * 포인트 순환 구조 방지를 위해 actualPaymentAmount 기준으로 변경됨.
 *
 * @see RewardBasisPolicy
 * @see PointAccrualPolicy
 */
@Component
public class CashbackAccrualPolicy implements RewardBasisPolicy {

    private static final String POLICY_NAME = "ACTUAL_PAYMENT_AMOUNT";
    private static final String POLICY_DESCRIPTION = "캐시백 적립은 실제 결제 금액(포인트 차감 후) 기준으로 계산됩니다.";

    /**
     * 캐시백 적립 기준 금액을 결정합니다.
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
