package org.example.sharedprompts.domain.payment.service.core;

import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentConfirmResponse;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    PaymentStatusResponseDto checkPaymentStatus(Long paymentId, Long userId);

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
    Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable);

    /**
     * 결제 승인 (토스페이먼츠 등 결제사별 승인 처리)
     */
    PaymentConfirmResponse confirmPayment(Long userId, PaymentConfirmRequest request);

    // ============ 관리자용 메서드 ============

    /**
     * 결제 상태 조회 (관리자용 - 소유권 검증 없음)
     */
    PaymentStatusResponseDto checkPaymentStatusForAdmin(Long paymentId);

    /**
     * 전체 결제 내역 조회 (관리자용)
     */
    Page<PaymentResponseDto> getAllPaymentHistory(Pageable pageable);

    /**
     * 결제 취소 (관리자용 - 소유권 검증 없음)
     */
    PaymentResponseDto cancelPaymentForAdmin(Long paymentId, PaymentCancelRequestDto request, Long adminId);

    /**
     * 결제 환불 (관리자용 - 소유권 검증 없음)
     */
    PaymentResponseDto refundPaymentForAdmin(Long paymentId, PaymentRefundRequestDto request, Long adminId);
}

