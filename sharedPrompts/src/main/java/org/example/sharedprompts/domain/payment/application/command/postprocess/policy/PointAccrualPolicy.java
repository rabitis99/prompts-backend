package org.example.sharedprompts.domain.payment.application.command.postprocess.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PointAccrualPolicy implements RewardBasisPolicy {

    private static final String POLICY_NAME = "ORIGINAL_ORDER_AMOUNT";
    private static final String POLICY_DESCRIPTION = "포인트 적립은 원래 주문 금액(포인트 차감 전) 기준으로 계산됩니다.";

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

