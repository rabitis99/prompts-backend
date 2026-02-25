package org.example.sharedprompts.dto.payment.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

/**
 * 결제 승인 요청 DTO
 */
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("amount")
    private long amount;

    @JsonProperty("payment_key")
    private String paymentKey;

    @JsonProperty("pg_token")
    private String pgToken;

    @JsonProperty("toss_order_id")
    private String tossOrderId;

    /**
     * 주문 ID를 Long으로 변환
     */
    public Long getOrderIdAsLong() {
        try {
            return Long.parseLong(this.orderId);
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "orderId",
                    "주문 ID는 숫자여야 합니다: " + this.orderId);
        }
    }

    /**
     * Payment ID (orderId와 동일)
     */
    public String getPaymentId() {
        return this.orderId;
    }

    /**
     * Provider Token (pgToken 또는 paymentKey)
     */
    public String getProviderToken() {
        if (this.pgToken != null) {
            return this.pgToken;
        }
        if (this.paymentKey != null) {
            return this.paymentKey;
        }
        throw new ApiException(
                ErrorCode.INVALID_INPUT_VALUE,
                "providerToken",
                "pgToken 또는 paymentKey 중 하나는 반드시 존재해야 합니다."
        );
    }

    /**
     * Raw Payload (JSON 형태의 원본 요청 데이터)
     */
    public String getRawPayload() {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("orderId", orderId);
        node.put("amount", amount);
        node.put("paymentKey", paymentKey);
        node.put("pgToken", pgToken);
        node.put("tossOrderId", tossOrderId);
        try {
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            throw new ApiException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "rawPayload",
                    "페이로드 직렬화에 실패했습니다."
            );
        }
    }
}

