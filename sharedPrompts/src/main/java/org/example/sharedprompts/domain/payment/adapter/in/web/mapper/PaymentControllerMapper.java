package org.example.sharedprompts.domain.payment.adapter.in.web.mapper;

import lombok.RequiredArgsConstructor;
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

/**
 * 결제 컨트롤러 매퍼
 * Controller DTO ↔ UseCase Command/Result 변환을 담당합니다.
 */
@Component
@RequiredArgsConstructor
public class PaymentControllerMapper {

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
     */
    public ConfirmPaymentCommand toConfirmCommand(PaymentConfirmRequest dto, Long userId) {
        return ConfirmPaymentCommand.builder()
                .paymentId(dto.getOrderIdAsLong())
                .userId(userId)
                .providerToken(dto.getProviderToken())
                .rawPayload(dto.getRawPayload())
                .build();
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
    public PaymentHistoryQuery toHistoryQuery(Long userId, org.springframework.data.domain.Pageable pageable) {
        return PaymentHistoryQuery.builder()
                .userId(userId)
                .pageable(pageable)
                .build();
    }

    /**
     * PaymentApprovalResult → PaymentResponseDto
     */
    public PaymentResponseDto toApprovalResponse(PaymentApprovalResult result) {
        return PaymentResponseDto.builder()
                .id(result.getPaymentId())
                .status(result.getStatus())
                .externalPaymentId(result.getExternalPaymentId())
                .amount(result.getAmount())
                .currency(result.getCurrency())
                .build();
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
     * List<PaymentHistoryResult> → Page<PaymentResponseDto> (내부적으로 사용)
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
