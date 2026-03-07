package org.example.sharedprompts.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 결제 준비(승인 요청) API 전용 응답 DTO.
 * POST /payments 응답에서만 사용하며, metadata는 항상 파싱된 객체 형태로 제공합니다.
 * 카카오페이 등 PG별 redirect_url, paymentData를 포함합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApprovalResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("status")
    private PaymentStatus status;

    @JsonProperty("external_payment_id")
    private String externalPaymentId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    /** 결제 준비 시 PG에서 내려준 메타데이터. 항상 객체(Map) 형태로 제공됨. */
    @JsonProperty("metadata")
    private Object metadata;

    /** 카카오페이 등 결제 준비 시 리다이렉트 URL (metadata.next_redirect_pc_url 또는 metadata.redirect_url) */
    @JsonProperty("redirect_url")
    private String redirectUrl;

    /** 결제사별 데이터. 카카오페이 시 redirect_url 등 (프론트 paymentData.redirect_url) */
    @JsonProperty("paymentData")
    private Map<String, Object> paymentData;
}
