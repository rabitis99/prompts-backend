package org.example.sharedprompts.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 포인트 내역 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointResponseDto {

    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("payment_id")
    private Long paymentId;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("balance")
    private BigDecimal balance;
    
    @JsonProperty("expired")
    private boolean expired;
    
    @JsonProperty("expired_at")
    private LocalDateTime expiredAt;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static PointResponseDto from(Point point) {
        return PointResponseDto.builder()
                .id(point.getId())
                .userId(point.getUser().getId())
                .paymentId(point.getPayment().getId())
                .amount(point.getAmount())
                .type(point.getType().name())
                .description(point.getDescription())
                .balance(point.getBalance())
                .expired(point.isExpired())
                .expiredAt(point.getExpiredAt())
                .createdAt(point.getCreatedAt())
                .updatedAt(point.getUpdatedAt())
                .build();
    }
}

