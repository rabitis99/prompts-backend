package org.example.sharedprompts.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 포인트 잔액 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointBalanceResponseDto {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("current_balance")
    private BigDecimal currentBalance;

    @JsonProperty("available_balance")
    private BigDecimal availableBalance; // 만료되지 않은 포인트만

    @JsonProperty("expiring_soon")
    private BigDecimal expiringSoon; // 곧 만료될 포인트 (30일 이내)

    /**
     * Create a PointBalanceResponseDto from the provided user ID and point amounts.
     *
     * @param userId the user's identifier
     * @param currentBalance the user's total point balance
     * @param availableBalance points that are currently available (not expired)
     * @param expiringSoon points that will expire soon (within 30 days)
     * @return a PointBalanceResponseDto populated with the given values
     */
    public static PointBalanceResponseDto from(Long userId, BigDecimal currentBalance, BigDecimal availableBalance, BigDecimal expiringSoon) {
        return PointBalanceResponseDto.builder()
                .userId(userId)
                .currentBalance(currentBalance)
                .availableBalance(availableBalance)
                .expiringSoon(expiringSoon)
                .build();
    }
}
