package org.example.sharedprompts.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

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
    private Integer totalAmount;
    
    @JsonProperty("approvedAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime approvedAt;
    
    @JsonProperty("method")
    private String method;
}

