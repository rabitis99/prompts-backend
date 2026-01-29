package org.example.sharedprompts.domain.payment.service.user;

import org.example.sharedprompts.domain.payment.enums.UserTier;
import org.example.sharedprompts.dto.payment.request.TierChangeRequestDto;
import org.example.sharedprompts.dto.payment.response.TierInfoResponseDto;

/**
 * 사용자 티어 서비스 인터페이스
 */
public interface UserTierService {

    /**
 * Retrieve the tier assigned to a user.
 *
 * @param userId the identifier of the user whose tier is requested
 * @return the user's current UserTier
 */
    UserTier getTier(Long userId);

    /**
 * Retrieve detailed tier information for the specified user, including tier, daily limit, uses today, and remaining uses.
 *
 * @param userId the id of the user whose tier information to retrieve
 * @return a TierInfoResponseDto containing the user's tier, daily limit, number of uses today, and remaining uses
 */
    TierInfoResponseDto getTierInfo(Long userId);

    /**
 * Change a user's tier (administrative operation).
 *
 * @param userId  the identifier of the user whose tier will be changed
 * @param request details of the requested tier change
 * @param changedBy the identifier of the administrator performing the change
 */
    void changeTier(Long userId, TierChangeRequestDto request, Long changedBy);

    /**
 * Recalculates a user's daily usage limit after their tier changes.
 *
 * @param userId the ID of the user whose daily limit will be recalculated
 */
    void recalculateDailyLimit(Long userId);

    /**
 * Retrieve the tier change history for the specified user.
 *
 * @param userId the identifier of the user whose tier history to retrieve
 * @return a list of UserTierHistoryResponseDto entries representing the user's past tier changes
 */
    java.util.List<org.example.sharedprompts.dto.payment.response.UserTierHistoryResponseDto> getTierHistory(Long userId);
}
