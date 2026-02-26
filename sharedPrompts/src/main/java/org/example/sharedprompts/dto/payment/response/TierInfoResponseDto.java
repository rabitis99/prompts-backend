package org.example.sharedprompts.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.enums.ModuleType;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.user.User;

import java.util.Map;

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

    @JsonProperty("remaining_by_module_type")
    private Map<String, Integer> remainingByModuleType;

    public static TierInfoResponseDto from(User user, int dailyLimit, int todayUsedCount, int remainingCount) {
        UserTier t = (user.getTier() != null) ? user.getTier() : UserTier.FREE;
        return TierInfoResponseDto.builder()
                .userId(user.getId())
                .tier(t)
                .tierDescription(t.getDescription())
                .dailyLimit(dailyLimit)
                .todayUsedCount(todayUsedCount)
                .remainingCount(remainingCount)
                .build();
    }

    public static TierInfoResponseDto from(User user, int dailyLimit, int todayUsedCount, int remainingCount,
                                          Map<String, Integer> remainingByModuleType) {
        UserTier t = (user.getTier() != null) ? user.getTier() : UserTier.FREE;
        return TierInfoResponseDto.builder()
                .userId(user.getId())
                .tier(t)
                .tierDescription(t.getDescription())
                .dailyLimit(dailyLimit)
                .todayUsedCount(todayUsedCount)
                .remainingCount(remainingCount)
                .remainingByModuleType(remainingByModuleType)
                .build();
    }

    public static TierInfoResponseDto fromModule(User user, ModuleType moduleType, int dailyLimit,
                                                 int todayUsedCount, int remainingCount) {
        UserTier t = (user.getTier() != null) ? user.getTier() : UserTier.FREE;
        return TierInfoResponseDto.builder()
                .userId(user.getId())
                .tier(t)
                .tierDescription(t.getDescription())
                .dailyLimit(dailyLimit)
                .todayUsedCount(todayUsedCount)
                .remainingCount(remainingCount)
                .build();
    }
}

