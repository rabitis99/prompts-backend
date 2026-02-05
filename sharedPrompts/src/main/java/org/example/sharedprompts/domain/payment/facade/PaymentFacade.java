package org.example.sharedprompts.domain.payment.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
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
 * <p><strong>역할 및 책임:</strong>
 * <ul>
 *   <li><strong>클라이언트 단일 진입점:</strong> Controller, Scheduler 등에서 사용하는 통합 인터페이스</li>
 *   <li><strong>트랜잭션 경계 관리:</strong> 각 메서드에서 적절한 트랜잭션 경계 설정</li>
 *   <li><strong>서비스 조율:</strong> 여러 Service/Facade를 조율하여 복잡한 비즈니스 로직 처리</li>
 * </ul>
 * 
 * <p><strong>내부 구조:</strong>
 * <ul>
 *   <li><strong>PaymentService:</strong> 결제 실행, 상태 조회, 내역 조회 등 핵심 비즈니스 로직</li>
 *   <li><strong>PaymentWebhookFacade:</strong> Webhook 파싱, 검증, 상태 변경 등 Webhook 전용 처리</li>
 *   <li><strong>PaymentRetryFacade:</strong> 재시도 상태 관리 및 재시도 실행</li>
 * </ul>
 * 
 * <p><strong>설계 의도:</strong>
 * <ul>
 *   <li>Controller가 여러 Service를 직접 호출하는 것을 방지하여 결합도 감소</li>
 *   <li>트랜잭션 경계를 Facade 레벨에서 명확히 관리</li>
 *   <li>향후 복잡한 비즈니스 로직 추가 시 Facade에서 조율 가능</li>
 * </ul>
 * 
 * <p><strong>참고:</strong>
 * 현재는 대부분의 메서드가 PaymentService를 단순 위임하고 있으나,
 * 향후 복잡한 비즈니스 로직(예: 결제 + 포인트 적립 + 알림 발송)이 추가될 경우
 * Facade에서 여러 서비스를 조율하는 역할을 수행할 수 있습니다.
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
     * Webhook 처리 (headers 포함)
     */
    @Transactional
    public Optional<Payment> handleWebhook(
            PaymentMethod paymentMethod,
            String payload,
            String signature,
            java.util.Map<String, String> headers
    ) {
        return webhookFacade.handleWebhook(paymentMethod, payload, signature, headers);
    }
    
    /**
     * 결제 재시도
     * 
     * <p>PaymentRetryFacade를 통한 재시도 처리
     * 
     * @param paymentId 결제 ID
     * @return PaymentResponseDto
     */
    public PaymentResponseDto retryPayment(Long paymentId) {
        retryFacade.retryPayment(paymentId);
        // 재시도 후 Payment 조회하여 PaymentResponseDto 생성
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        return PaymentResponseDto.from(payment);
    }
}

