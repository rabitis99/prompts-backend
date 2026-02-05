package org.example.sharedprompts.domain.payment.application.command.postprocess.policy;

import java.math.BigDecimal;

public interface RewardBasisPolicy {

    BigDecimal determineBasisAmount(BigDecimal originalAmount, BigDecimal actualPaymentAmount, BigDecimal usedPointAmount);

    String getPolicyName();

    String getPolicyDescription();
}

