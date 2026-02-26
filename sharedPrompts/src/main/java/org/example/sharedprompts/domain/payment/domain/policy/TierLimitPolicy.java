package org.example.sharedprompts.domain.payment.domain.policy;

import org.example.sharedprompts.domain.payment.domain.enums.ModuleType;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;

public interface TierLimitPolicy {

    int getDailyLimit(UserTier tier);

    /**
     * 해당 모듈 1회 사용 시 통합 한도에서 차감되는 양. 모듈마다 다를 수 있음.
     */
    int getConsumptionAmount(ModuleType moduleType);
}
