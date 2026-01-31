package org.example.sharedprompts.domain.payment.service.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.model.PaymentResult;
import org.example.sharedprompts.domain.payment.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.service.validation.PaymentValidationService;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 상태 동기화 서비스
 * 
 * <p>단일 책임: 외부 API 상태 조회 및 DB 상태 동기화만 담당
 * - 외부 Provider에서 결제 상태 조회
 * - PaymentResult 기반 상태 동기화
 * - Payment 도메인 메서드를 통한 상태 변경
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStatusSyncService {

    private final PaymentProviderFactory providerFactory;
    private final PaymentRepository paymentRepository;
    private final PaymentValidationService validationService;

    /**
     * 사용자용 결제 상태 동기화
     */
    @Transactional
    public PaymentStatusResponseDto syncPaymentStatus(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        validationService.validatePaymentOwnership(payment, userId);
        
        // Provider 선택 및 상태 조회
        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());
        PaymentResult result = provider.getPaymentStatus(payment.getExternalPaymentId());
        
        // 상태 동기화 (도메인 중심)
        if (payment.getStatus() != result.getStatus()) {
            payment = paymentRepository.findById(payment.getId()).orElse(payment);
            syncPaymentStatusFromResult(payment, result);
            paymentRepository.save(payment);
        }

        return PaymentStatusResponseDto.from(payment);
    }

    /**
     * 관리자용 결제 상태 동기화
     */
    @Transactional
    public PaymentStatusResponseDto syncPaymentStatusForAdmin(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        // Provider 선택 및 상태 조회
        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());
        PaymentResult result = provider.getPaymentStatus(payment.getExternalPaymentId());
        
        // 상태 동기화 (도메인 중심)
        if (payment.getStatus() != result.getStatus()) {
            payment = paymentRepository.findById(payment.getId()).orElse(payment);
            syncPaymentStatusFromResult(payment, result);
            paymentRepository.save(payment);
        }

        return PaymentStatusResponseDto.from(payment);
    }

    /**
     * PaymentResult를 기반으로 Payment 상태 동기화
     * 도메인 중심 상태 변경 (Payment.markSuccess()/markFailed()/markCanceled() 호출)
     */
    @Transactional
    public void syncPaymentStatusFromResult(Payment payment, PaymentResult result) {
        PaymentStatus currentStatus = payment.getStatus();
        PaymentStatus latestStatus = result.getStatus();
        
        // 이미 동일한 상태면 처리하지 않음
        if (currentStatus == latestStatus) {
            return;
        }
        
        switch (latestStatus) {
            case SUCCESS:
                // PENDING -> SUCCESS 전이만 처리
                if (currentStatus == PaymentStatus.PENDING) {
                    payment.approve(result.getExternalPaymentId());
                    log.info("외부 결제사 상태 동기화: paymentId={}, {} -> {}", 
                            payment.getId(), currentStatus, latestStatus);
                }
                break;
                
            case FAILED:
                // PENDING -> FAILED 전이만 처리
                if (currentStatus == PaymentStatus.PENDING) {
                    payment.fail(result.getFailureReason() != null ? result.getFailureReason() : "외부 결제사에서 결제 실패로 확인됨");
                    log.info("외부 결제사 상태 동기화: paymentId={}, {} -> {}", 
                            payment.getId(), currentStatus, latestStatus);
                }
                break;
                
            case CANCELED:
                // SUCCESS, PENDING -> CANCELED 전이 처리
                if (currentStatus == PaymentStatus.SUCCESS || currentStatus == PaymentStatus.PENDING) {
                    payment.cancel();
                    log.info("외부 결제사 상태 동기화: paymentId={}, {} -> {}", 
                            payment.getId(), currentStatus, latestStatus);
                }
                break;
                
            default:
                log.warn("알 수 없는 결제 상태: paymentId={}, 상태={}", payment.getId(), latestStatus);
                break;
        }
    }
}

