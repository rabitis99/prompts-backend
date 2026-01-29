package org.example.sharedprompts.domain.payment.service.core;

import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;

/**
 * 결제 서비스 인터페이스
 */
public interface PaymentService {

    /**
     * 결제 요청
     * 티어 기반 일일 제한 체크 포함
     */
    PaymentResponseDto requestPayment(Long userId, PaymentRequestDto request);

    /**
     * 결제 상태 조회
     */
    PaymentStatusResponseDto checkPaymentStatus(String paymentId);

    /**
     * 결제 취소
     */
    PaymentResponseDto cancelPayment(Long userId, PaymentCancelRequestDto request);

    /**
     * 결제 환불 (부분/전체)
     */
    PaymentResponseDto refundPayment(Long userId, PaymentRefundRequestDto request);

    /**
     * 사용자의 결제 내역 조회
     */
    java.util.List<PaymentResponseDto> getPaymentHistory(Long userId);
}

