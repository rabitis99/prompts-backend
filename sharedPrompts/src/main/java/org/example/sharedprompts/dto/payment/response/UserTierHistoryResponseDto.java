package org.example.sharedprompts.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.UserTierHistory;
import org.example.sharedprompts.domain.payment.enums.UserTier;

import java.time.LocalDateTime;

/**
 * 사용자 티어 변경 이력 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTierHistoryResponseDto {

    private Long id;
    
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("previous_tier")
    private UserTier previousTier;
    
    @JsonProperty("new_tier")
    private UserTier newTier;
    
    @JsonProperty("changed_by")
    private Long changedBy;
    
    private String reason;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    /**
     * Create a UserTierHistoryResponseDto populated from a UserTierHistory domain object.
     *
     * @param history the domain object containing user tier change details
     * @return a UserTierHistoryResponseDto populated with values from {@code history}
     */
    public static UserTierHistoryResponseDto from(UserTierHistory history) {
        return UserTierHistoryResponseDto.builder()
                .id(history.getId())
                .userId(history.getUser().getId())
                .previousTier(history.getPreviousTier())
                .newTier(history.getNewTier())
                .changedBy(history.getChangedBy())
                .reason(history.getReason())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
