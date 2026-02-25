package org.example.sharedprompts.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 결제 승인 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PaymentConfirmResponse {

    @JsonProperty("payment_id")
    private Long paymentId;

    @JsonProperty("payment_key")
    private String paymentKey;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("total_amount")
    private Integer totalAmount;

    @JsonProperty("amount")
    private java.math.BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("external_payment_id")
    private String externalPaymentId;

    @JsonProperty("approved_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime approvedAt;

    @JsonProperty("method")
    private String method;
}

