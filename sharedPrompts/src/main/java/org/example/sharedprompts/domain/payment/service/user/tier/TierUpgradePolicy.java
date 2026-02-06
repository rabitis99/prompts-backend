package org.example.sharedprompts.domain.payment.service.user.tier;

import org.example.sharedprompts.domain.payment.domain.enums.UserTier;

import java.math.BigDecimal;

public interface TierUpgradePolicy {
    
    UserTier calculateTier(BigDecimal totalPaymentAmount, UserTier currentTier);
    
    boolean shouldUpgrade(BigDecimal totalPaymentAmount, UserTier currentTier);
}

