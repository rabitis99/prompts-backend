package org.example.sharedprompts.domain.payment.service.user.tier;

import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;


@Component
public class TierUpgradePolicyImpl implements TierUpgradePolicy {

    private static final BigDecimal PRO_TIER_THRESHOLD = new BigDecimal("9900");
    private static final BigDecimal PREMIUM_TIER_THRESHOLD = new BigDecimal("10000");

    @Override
    public UserTier calculateTier(BigDecimal totalPaymentAmount, UserTier currentTier) {
        if (totalPaymentAmount == null) {
            return currentTier;
        }

        if (totalPaymentAmount.compareTo(PREMIUM_TIER_THRESHOLD) >= 0) {
            return UserTier.PREMIUM;
        } else if (totalPaymentAmount.compareTo(PRO_TIER_THRESHOLD) >= 0) {
            return UserTier.PRO;
        } else {
            return UserTier.FREE;
        }
    }

    @Override
    public boolean shouldUpgrade(BigDecimal totalPaymentAmount, UserTier currentTier) {
        UserTier calculatedTier = calculateTier(totalPaymentAmount, currentTier);
        return calculatedTier.getDailyLimit() > currentTier.getDailyLimit();
    }
}

