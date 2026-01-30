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
     * 사용자 티어 정보를 DTO로 변환
     */
    public static TierInfoResponseDto from(User user, int dailyLimit, int todayUsedCount, int remainingCount) {
        UserTier tier = (user.getTier() != null) ? user.getTier() : UserTier.FREE;
        return TierInfoResponseDto.builder()
                .userId(user.getId())
                .tier(tier)
                .tierDescription(tier.getDescription())
                .dailyLimit(dailyLimit)
                .todayUsedCount(todayUsedCount)
                .remainingCount(remainingCount)
                .build();
    }
}

