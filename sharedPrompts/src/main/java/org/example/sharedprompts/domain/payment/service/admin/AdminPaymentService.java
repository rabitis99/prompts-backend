package org.example.sharedprompts.domain.payment.service.admin;

import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 관리자용 결제 서비스 인터페이스
 * 
 * <p>관리자 전용 기능을 제공합니다:
 * <ul>
 *   <li>소유권 검증 없이 모든 결제 조회/취소/환불 가능</li>
 *   <li>전체 결제 내역 조회</li>
 * </ul>
 */
public interface AdminPaymentService {

    /**
     * 결제 상태 조회 (관리자용 - 소유권 검증 없음)
     * 
     * @param paymentId 결제 ID
     * @return 결제 상태 정보
     */
    PaymentStatusResponseDto checkPaymentStatus(Long paymentId);

    /**
     * 전체 결제 내역 조회 (관리자용)
     * 
     * @param pageable 페이징 정보
     * @return 전체 결제 내역
     */
    Page<PaymentResponseDto> getAllPaymentHistory(Pageable pageable);

    /**
     * 결제 취소 (관리자용 - 소유권 검증 없음)
     * 
     * @param paymentId 결제 ID
     * @param request 취소 요청 DTO
     * @param adminId 관리자 ID
     * @return 취소된 결제 정보
     */
    PaymentResponseDto cancelPayment(Long paymentId, PaymentCancelRequestDto request, Long adminId);

    /**
     * 결제 환불 (관리자용 - 소유권 검증 없음)
     * 
     * @param paymentId 결제 ID
     * @param request 환불 요청 DTO
     * @param adminId 관리자 ID
     * @return 환불된 결제 정보
     */
    PaymentResponseDto refundPayment(Long paymentId, PaymentRefundRequestDto request, Long adminId);
}

