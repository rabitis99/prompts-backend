package org.example.sharedprompts.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.Cashback;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 캐시백 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashbackResponseDto {

    private Long id;
    
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("payment_id")
    private Long paymentId;
    
    private BigDecimal amount;
    
    private BigDecimal rate;
    
    @JsonProperty("payment_amount")
    private BigDecimal paymentAmount;
    
    private String description;
    
    private boolean paid;
    
    @JsonProperty("paid_at")
    private LocalDateTime paidAt;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Create a CashbackResponseDto from a Cashback domain object.
     *
     * @param cashback the Cashback domain instance to convert
     * @return a CashbackResponseDto populated with the cashback's id, user id, payment id, amount, rate,
     *         payment amount, description, paid flag, and timestamp fields
     */
    public static CashbackResponseDto from(Cashback cashback) {
        return CashbackResponseDto.builder()
                .id(cashback.getId())
                .userId(cashback.getUser().getId())
                .paymentId(cashback.getPaymentId())
                .amount(cashback.getAmount())
                .rate(cashback.getRate())
                .paymentAmount(cashback.getPaymentAmount())
                .description(cashback.getDescription())
                .paid(cashback.isPaid())
                .paidAt(cashback.getPaidAt())
                .createdAt(cashback.getCreatedAt())
                .updatedAt(cashback.getUpdatedAt())
                .build();
    }
}
