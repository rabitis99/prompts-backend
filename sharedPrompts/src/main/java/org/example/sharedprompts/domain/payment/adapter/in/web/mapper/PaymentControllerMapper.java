package org.example.sharedprompts.domain.payment.adapter.in.web.mapper;

import org.example.sharedprompts.domain.payment.application.command.service.metadata.PaymentMetadataKeys;
import org.example.sharedprompts.domain.payment.application.command.service.metadata.PaymentMetadataParser;
import org.example.sharedprompts.domain.payment.application.port.in.command.*;
import org.example.sharedprompts.domain.payment.application.port.in.result.*;
import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentConfirmResponse;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Pageable;

import lombok.RequiredArgsConstructor;

/**
 * 결제 컨트롤러 매퍼
 * Controller DTO ↔ UseCase Command/Result 변환을 담당합니다.
 */
@Component
@RequiredArgsConstructor
public class PaymentControllerMapper {

    private final PaymentMetadataParser metadataParser;

    /**
     * PaymentRequestDto → ApprovePaymentCommand
     */
    public ApprovePaymentCommand toApproveCommand(PaymentRequestDto dto, Long userId) {
        return ApprovePaymentCommand.builder()
                .userId(userId)
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .paymentMethod(dto.getPaymentMethod())
                .usePointAmount(dto.getUsePointAmount() != null ? dto.getUsePointAmount() : BigDecimal.ZERO)
                .userType(dto.getUserType())
                .metadata(dto.getMetadata())
                .build();
    }

    /**
     * PaymentConfirmRequest → ConfirmPaymentCommand
     * pg_token, toss_order_id 등 PG별 값을 additionalParams에 담아 전달.
     */
    public ConfirmPaymentCommand toConfirmCommand(PaymentConfirmRequest dto, Long userId) {
        Map<String, String> additionalParams = buildConfirmAdditionalParams(dto);
        return ConfirmPaymentCommand.builder()
                .paymentId(dto.getOrderIdAsLong())
                .userId(userId)
                .providerToken(dto.getProviderToken())
                .rawPayload(dto.getRawPayload())
                .additionalParams(additionalParams)
                .build();
    }

    private static Map<String, String> buildConfirmAdditionalParams(PaymentConfirmRequest dto) {
        Map<String, String> params = new HashMap<>();
        if (dto.getPgToken() != null && !dto.getPgToken().isBlank()) {
            params.put("pgToken", dto.getPgToken());
        }
        if (dto.getTossOrderId() != null && !dto.getTossOrderId().isBlank()) {
            params.put("tossOrderId", dto.getTossOrderId());
        }
        return params.isEmpty() ? Collections.emptyMap() : params;
    }

    /**
     * PaymentCancelRequestDto → CancelPaymentCommand
     */
    public CancelPaymentCommand toCancelCommand(PaymentCancelRequestDto dto, Long userId) {
        return CancelPaymentCommand.builder()
                .paymentId(dto.getPaymentIdAsLong())
                .userId(userId)
                .reason(dto.getReasonOrDefault())
                .build();
    }

    /**
     * PaymentRefundRequestDto → RefundPaymentCommand
     */
    public RefundPaymentCommand toRefundCommand(PaymentRefundRequestDto dto, Long userId) {
        return RefundPaymentCommand.builder()
                .paymentId(dto.getPaymentIdAsLong())
                .userId(userId)
                .refundAmount(dto.getRefundAmount())
                .reason(dto.getReasonOrDefault())
                .build();
    }

    /**
     * PaymentStatusCheckQuery
     */
    public PaymentStatusCheckQuery toStatusCheckQuery(Long paymentId, Long userId) {
        return PaymentStatusCheckQuery.builder()
                .paymentId(paymentId)
                .userId(userId)
                .build();
    }

    /**
     * PaymentHistoryQuery
     */
    public PaymentHistoryQuery toHistoryQuery(Long userId, Pageable pageable) {
        return PaymentHistoryQuery.builder()
                .userId(userId)
                .pageable(pageable)
                .build();
    }

    /**
     * PaymentApprovalResult → PaymentResponseDto
     * 카카오페이 등 결제 준비 시 metadata(객체), redirect_url, payment_data.redirect_url 포함.
     */
    public PaymentResponseDto toApprovalResponse(PaymentApprovalResult result) {
        PaymentResponseDto.PaymentResponseDtoBuilder builder = PaymentResponseDto.builder()
                .id(result.getPaymentId())
                .status(result.getStatus())
                .externalPaymentId(result.getExternalPaymentId())
                .amount(result.getAmount())
                .currency(result.getCurrency());

        if (result.getMetadata() != null && !result.getMetadata().isBlank()) {
            metadataParser.parseMetadata(result.getMetadata()).ifPresent(metaMap -> {
                builder.metadata(metaMap);
                String redirectUrl = getRedirectUrlFromMeta(metaMap);
                if (redirectUrl != null) {
                    builder.redirectUrl(redirectUrl);
                    builder.paymentData(Collections.singletonMap("redirect_url", redirectUrl));
                }
            });
        }

        return builder.build();
    }

    private static String getRedirectUrlFromMeta(Map<String, Object> metaMap) {
        Object url = metaMap.get(PaymentMetadataKeys.NEXT_REDIRECT_PC_URL);
        if (url != null && url.toString().length() > 0) {
            return url.toString();
        }
        url = metaMap.get(PaymentMetadataKeys.REDIRECT_URL);
        return (url != null && url.toString().length() > 0) ? url.toString() : null;
    }

    /**
     * PaymentConfirmationResult → PaymentConfirmResponse
     */
    public PaymentConfirmResponse toConfirmationResponse(PaymentConfirmationResult result) {
        return PaymentConfirmResponse.builder()
                .paymentId(result.getPaymentId())
                .status(result.getStatus().name())
                .externalPaymentId(result.getExternalPaymentId())
                .amount(result.getAmount())
                .currency(result.getCurrency())
                .build();
    }

    /**
     * PaymentCancellationResult → PaymentResponseDto
     */
    public PaymentResponseDto toCancellationResponse(PaymentCancellationResult result) {
        return PaymentResponseDto.builder()
                .id(result.getPaymentId())
                .status(result.getStatus())
                .refundedAmount(result.getRefundedAmount())
                .build();
    }

    /**
     * PaymentRefundResult → PaymentResponseDto
     */
    public PaymentResponseDto toRefundResponse(PaymentRefundResult result) {
        return PaymentResponseDto.builder()
                .id(result.getPaymentId())
                .status(result.getStatus())
                .refundedAmount(result.getRefundAmount())
                .build();
    }

    /**
     * PaymentStatusResult → PaymentStatusResponseDto
     */
    public PaymentStatusResponseDto toStatusResponse(PaymentStatusResult result) {
        return PaymentStatusResponseDto.builder()
                .id(result.getId())
                .userId(result.getUserId())
                .status(result.getStatus())
                .amount(result.getAmount())
                .currency(result.getCurrency())
                .createdAt(result.getCreatedAt())
                .approvedAt(result.getApprovedAt())
                .canceledAt(result.getCanceledAt())
                .build();
    }

    /**
     * PaymentHistoryResult → PaymentResponseDto (단건 매핑)
     */
    public PaymentResponseDto toHistoryItemResponse(PaymentHistoryResult result) {
        return PaymentResponseDto.builder()
                .id(result.getId())
                .amount(result.getAmount())
                .currency(result.getCurrency())
                .status(result.getStatus())
                .createdAt(result.getCreatedAt())
                .approvedAt(result.getApprovedAt())
                .build();
    }
}
