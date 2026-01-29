package org.example.sharedprompts.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.enums.UserTier;
import org.example.sharedprompts.domain.user.User;

/**
 * 사용자 티어 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierInfoResponseDto {

    @JsonProperty("user_id")
    private Long userId;

    private UserTier tier;

    @JsonProperty("tier_description")
    private String tierDescription;

    @JsonProperty("daily_limit")
    private int dailyLimit;

    @JsonProperty("today_used_count")
    private int todayUsedCount;

    @JsonProperty("remaining_count")
    private int remainingCount;

    /**
     * Create a TierInfoResponseDto populated from the given User and usage counts.
     *
     * @param user            the domain User whose id, tier, and tier description will be used
     * @param dailyLimit      the total allowed uses per day for the user's tier
     * @param todayUsedCount  the number of uses consumed today
     * @param remainingCount  the number of remaining uses available today
     * @return                a TierInfoResponseDto populated with the user's id, tier, tier description, and provided counts
     */
    public static TierInfoResponseDto from(User user, int dailyLimit, int todayUsedCount, int remainingCount) {
        return TierInfoResponseDto.builder()
                .userId(user.getId())
                .tier(user.getTier())
                .tierDescription(user.getTier().getDescription())
                .dailyLimit(dailyLimit)
                .todayUsedCount(todayUsedCount)
                .remainingCount(remainingCount)
                .build();
    }
}
