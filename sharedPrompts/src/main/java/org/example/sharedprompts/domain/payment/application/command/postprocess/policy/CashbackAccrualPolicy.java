package org.example.sharedprompts.domain.payment.application.command.postprocess.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class CashbackAccrualPolicy implements RewardBasisPolicy {

    private static final String POLICY_NAME = "ACTUAL_PAYMENT_AMOUNT";
    private static final String POLICY_DESCRIPTION = "캐시백 적립은 실제 결제 금액(포인트 차감 후) 기준으로 계산됩니다.";

    @Override
    public BigDecimal determineBasisAmount(BigDecimal originalAmount, BigDecimal actualPaymentAmount, BigDecimal usedPointAmount) {
        return Objects.requireNonNullElse(actualPaymentAmount, BigDecimal.ZERO);
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

