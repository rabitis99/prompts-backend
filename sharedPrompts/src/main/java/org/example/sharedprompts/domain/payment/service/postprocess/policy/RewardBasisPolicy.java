package org.example.sharedprompts.domain.payment.service.postprocess.policy;

import java.math.BigDecimal;

/**
 * 리워드(포인트/캐시백) 적립 기준 금액 결정 정책 인터페이스
 *
 * <p>결제 완료 시 포인트/캐시백 적립의 기준이 되는 금액을 결정합니다.
 * 이 인터페이스를 통해 적립 정책을 명시적으로 분리하고 테스트 가능하게 만듭니다.
 *
 * <p><strong>정책 결정 시 고려사항:</strong>
 * <ul>
 *   <li>originalAmount: 포인트 차감 전 원래 주문 금액</li>
 *   <li>actualPaymentAmount: 포인트 차감 후 실제 결제 금액</li>
 * </ul>
 *
 * @see PointAccrualPolicy
 * @see CashbackAccrualPolicy
 */
public interface RewardBasisPolicy {

    /**
     * 리워드 적립 기준 금액을 결정합니다.
     *
     * @param originalAmount 포인트 차감 전 원래 주문 금액
     * @param actualPaymentAmount 포인트 차감 후 실제 결제 금액
     * @param usedPointAmount 사용된 포인트 금액
     * @return 리워드 적립 기준 금액
     */
    BigDecimal determineBasisAmount(BigDecimal originalAmount, BigDecimal actualPaymentAmount, BigDecimal usedPointAmount);

    /**
     * 이 정책의 이름을 반환합니다. 로깅 및 디버깅 목적.
     *
     * @return 정책 이름
     */
    String getPolicyName();

    /**
     * 이 정책의 설명을 반환합니다.
     *
     * @return 정책 설명
     */
    String getPolicyDescription();
}
