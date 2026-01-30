package org.example.sharedprompts.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 토스페이먼츠 결제 승인 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class PaymentConfirmResponse {
    
    @JsonProperty("paymentKey")
    private String paymentKey;
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("totalAmount")
    private int totalAmount;
    
    @JsonProperty("approvedAt")
    private LocalDateTime approvedAt;
    
    @JsonProperty("method")
    private String method;
}

