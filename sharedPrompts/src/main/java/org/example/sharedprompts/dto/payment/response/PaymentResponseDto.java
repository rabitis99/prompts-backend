package org.example.sharedprompts.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.enums.PaymentUserType;
import org.example.sharedprompts.domain.payment.enums.UserTier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("payment_method")
    private PaymentMethod paymentMethod;
    
    @JsonProperty("status")
    private PaymentStatus status;
    
    @JsonProperty("user_type")
    private PaymentUserType userType;
    
    @JsonProperty("tier")
    private UserTier tier;
    
    @JsonProperty("external_payment_id")
    private String externalPaymentId;
    
    @JsonProperty("failure_reason")
    private String failureReason;
    
    @JsonProperty("retry_count")
    private int retryCount;
    
    @JsonProperty("approved_at")
    private LocalDateTime approvedAt;
    
    @JsonProperty("canceled_at")
    private LocalDateTime canceledAt;
    
    @JsonProperty("refunded_amount")
    private BigDecimal refundedAmount;
    
    @JsonProperty("refundable_amount")
    private BigDecimal refundableAmount;
    
    @JsonProperty("used_point_amount")
    private BigDecimal usedPointAmount;
    
    @JsonProperty("metadata")
    private String metadata;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static PaymentResponseDto from(Payment payment) {
        return PaymentResponseDto.builder()
                .id(payment.getId())
                .userId(payment.getUser().getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .userType(payment.getUserType())
                .tier(payment.getTier())
                .externalPaymentId(payment.getExternalPaymentId())
                .failureReason(payment.getFailureReason())
                .retryCount(payment.getRetryCount())
                .approvedAt(payment.getApprovedAt())
                .canceledAt(payment.getCanceledAt())
                .refundedAmount(payment.getRefundedAmount())
                .refundableAmount(payment.getRefundableAmount())
                .usedPointAmount(payment.getUsedPointAmount())
                .metadata(payment.getMetadata())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}

