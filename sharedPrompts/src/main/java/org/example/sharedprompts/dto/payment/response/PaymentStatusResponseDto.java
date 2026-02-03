package org.example.sharedprompts.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;

import java.time.LocalDateTime;

/**
 * 결제 상태 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponseDto {

    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("status")
    private PaymentStatus status;
    
    @JsonProperty("external_payment_id")
    private String externalPaymentId;
    
    @JsonProperty("failure_reason")
    private String failureReason;
    
    @JsonProperty("approved_at")
    private LocalDateTime approvedAt;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static PaymentStatusResponseDto from(Payment payment) {
        return PaymentStatusResponseDto.builder()
                .id(payment.getId())
                .status(payment.getStatus())
                .externalPaymentId(payment.getExternalPaymentId())
                .failureReason(payment.getFailureReason())
                .approvedAt(payment.getApprovedAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}

