package org.example.sharedprompts.domain.payment.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.facade.PaymentRetryFacade;
import org.example.sharedprompts.domain.payment.facade.PaymentWebhookFacade;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.core.PaymentService;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.dto.payment.request.PaymentCancelRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentConfirmRequest;
import org.example.sharedprompts.dto.payment.request.PaymentRefundRequestDto;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.example.sharedprompts.dto.payment.response.PaymentConfirmResponse;
import org.example.sharedprompts.dto.payment.response.PaymentResponseDto;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Payment Facade
 * 
 * <p>클라이언트(Controller, Scheduler 등)의 단일 진입점
 * - 결제 실행, Webhook 처리, 검증, 재시도 등 통합 제공
 * - 내부적으로 세분화된 Service/Facade 호출:
 *   - PaymentService: 결제 실행, 상태 조회, 내역 조회 등
 *   - PaymentWebhookFacade: Webhook 처리
 *   - PaymentRetryFacade: 재시도 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {
    
    private final PaymentService paymentService;
    private final PaymentWebhookFacade webhookFacade;
    private final PaymentRetryFacade retryFacade;
    private final PaymentRepository paymentRepository;
    
    /**
     * 결제 요청
     * 
     * @param userId 사용자 ID
     * @param request 결제 요청 DTO
     * @return PaymentResponseDto
     */
    @Transactional
    public PaymentResponseDto requestPayment(Long userId, PaymentRequestDto request) {
        return paymentService.requestPayment(userId, request);
    }
    
    /**
     * 결제 실행 (별칭 - requestPayment와 동일)
     * 
     * @param userId 사용자 ID
     * @param request 결제 요청 DTO
     * @return PaymentResponseDto
     */
    @Transactional
    public PaymentResponseDto executePayment(Long userId, PaymentRequestDto request) {
        return requestPayment(userId, request);
    }
    
    /**
     * 결제 상태 조회
     * 
     * @param paymentId 결제 ID
     * @param userId 사용자 ID
     * @return PaymentStatusResponseDto
     */
    @Transactional(readOnly = true)
    public PaymentStatusResponseDto checkPaymentStatus(Long paymentId, Long userId) {
        return paymentService.checkPaymentStatus(paymentId, userId);
    }
    
    /**
     * 결제 취소
     * 
     * @param userId 사용자 ID
     * @param request 취소 요청 DTO
     * @return PaymentResponseDto
     */
    @Transactional
    public PaymentResponseDto cancelPayment(Long userId, PaymentCancelRequestDto request) {
        return paymentService.cancelPayment(userId, request);
    }
    
    /**
     * 결제 환불
     * 
     * @param userId 사용자 ID
     * @param request 환불 요청 DTO
     * @return PaymentResponseDto
     */
    @Transactional
    public PaymentResponseDto refundPayment(Long userId, PaymentRefundRequestDto request) {
        return paymentService.refundPayment(userId, request);
    }
    
    /**
     * 결제 승인 (토스페이먼츠 등 결제사별 승인 처리)
     * 
     * @param userId 사용자 ID
     * @param request 승인 요청 DTO
     * @return PaymentConfirmResponse
     */
    @Transactional
    public PaymentConfirmResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        return paymentService.confirmPayment(userId, request);
    }
    
    /**
     * 사용자의 결제 내역 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return Page<PaymentResponseDto>
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable) {
        return paymentService.getPaymentHistory(userId, pageable);
    }
    
    /**
     * Webhook 처리
     * 
     * <p>PaymentWebhookFacade를 통한 Webhook 처리
     * 
     * @param paymentMethod 결제 수단
     * @param payload Webhook 페이로드
     * @param signature 서명 (검증용)
     * @return 처리된 Payment (이미 처리된 경우 기존 Payment 반환)
     */
    @Transactional
    public Optional<Payment> handleWebhook(PaymentMethod paymentMethod, String payload, String signature) {
        return webhookFacade.handleWebhook(paymentMethod, payload, signature);
    }
    
    /**
     * 결제 재시도
     * 
     * <p>PaymentRetryFacade를 통한 재시도 처리
     * 
     * @param paymentId 결제 ID
     * @return PaymentResponseDto
     */
    @Transactional
    public PaymentResponseDto retryPayment(Long paymentId) {
        retryFacade.retryPayment(paymentId);
        // 재시도 후 Payment 조회하여 PaymentResponseDto 생성
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        return PaymentResponseDto.from(payment);
    }
}

