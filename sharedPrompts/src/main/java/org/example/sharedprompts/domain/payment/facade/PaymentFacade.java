package org.example.sharedprompts.domain.payment.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.service.core.PaymentService;
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

import java.util.Map;
import java.util.Optional;

/**
 * Payment Facade
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {
    
    private final PaymentService paymentService;
    private final PaymentWebhookFacade webhookFacade;
    private final PaymentRetryFacade retryFacade;
    
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
     */
    @Transactional
    public Optional<Payment> handleWebhook(PaymentMethod paymentMethod, String payload, String signature) {
        return webhookFacade.handleWebhook(paymentMethod, payload, signature);
    }

    /**
     * Webhook 처리 (headers 포함)
     */
    @Transactional
    public Optional<Payment> handleWebhook(
            PaymentMethod paymentMethod,
            String payload,
            String signature,
            Map<String, String> headers
    ) {
        return webhookFacade.handleWebhook(paymentMethod, payload, signature, headers);
    }
    
    /**
     * 결제 재시도
     * 
     * @param paymentId 결제 ID
     * @return PaymentResponseDto
     */
    @Transactional
    public PaymentResponseDto retryPayment(Long paymentId) {
        return retryFacade.retryPaymentAsResponse(paymentId);
    }
}

