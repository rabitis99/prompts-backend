package org.example.sharedprompts.domain.payment.service.user.tier;

import org.example.sharedprompts.domain.payment.domain.enums.ModuleType;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.dto.payment.request.TierChangeRequestDto;
import org.example.sharedprompts.dto.payment.response.ConsumeModuleUsageResponseDto;
import org.example.sharedprompts.dto.payment.response.TierInfoResponseDto;
import org.example.sharedprompts.dto.payment.response.UserTierHistoryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface UserTierService {

    UserTier getTier(Long userId);

    TierInfoResponseDto getTierInfo(Long userId);

    TierInfoResponseDto getTierInfo(Long userId, ModuleType moduleType);

    long getTodayUsedCount(Long userId);

    long getTodayPaymentCount(Long userId);

    void changeTier(Long userId, TierChangeRequestDto request, Long changedBy);

    void recalculateDailyLimit(Long userId);

    Page<UserTierHistoryResponseDto> getTierHistory(Long userId, Pageable pageable);

    void upgradeTierIfEligible(Long userId, BigDecimal paymentAmount);

    ConsumeModuleUsageResponseDto consumeModuleUsage(Long userId, ModuleType moduleType);
}

