package org.example.sharedprompts.domain.payment.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.application.command.PaymentValidationService;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStatusSyncService {

    private final PaymentProviderFactory providerFactory;
    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentValidationService validationService;

    @Transactional
    public PaymentStatusResponseDto syncPaymentStatus(Long paymentId, Long userId) {
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        validationService.validatePaymentOwnership(payment, userId);

        return doSyncPaymentStatus(payment);
    }

    @Transactional
    public PaymentStatusResponseDto syncPaymentStatusForAdmin(Long paymentId) {
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        return doSyncPaymentStatus(payment);
    }

    private PaymentStatusResponseDto doSyncPaymentStatus(Payment payment) {
        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());
        PaymentResult result = provider.getPaymentStatus(payment.getExternalPaymentId());

        if (payment.getStatus() != result.getStatus()) {
            syncPaymentStatusFromResult(payment, result);
            try {
                paymentJpaAdapter.save(payment);
            } catch (OptimisticLockingFailureException e) {
                // 외부 API 호출 중 다른 곳에서 Payment가 수정된 경우
                // 최신 상태로 재조회하여 재시도
                log.warn("낙관적 락 충돌 발생, 최신 상태로 재조회: paymentId={}", payment.getId());
                Payment freshPayment = paymentJpaAdapter.findById(payment.getId())
                        .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
                
                // 상태가 이미 변경되었는지 확인
                if (freshPayment.getStatus() == result.getStatus()) {
                    log.info("상태가 이미 동기화됨: paymentId={}, status={}", payment.getId(), freshPayment.getStatus());
                    return PaymentStatusResponseDto.from(freshPayment);
                }
                
                // 상태가 다르면 재시도
                syncPaymentStatusFromResult(freshPayment, result);
                paymentJpaAdapter.save(freshPayment);
            }
        }

        return PaymentStatusResponseDto.from(payment);
    }

    private void syncPaymentStatusFromResult(Payment payment, PaymentResult result) {
        PaymentStatus currentStatus = payment.getStatus();
        PaymentStatus latestStatus = result.getStatus();
        if (currentStatus == latestStatus) {
            return;
        }
        
        switch (latestStatus) {
            case SUCCESS:
                if (currentStatus.isPending()) {
                    payment.approve(result.getExternalPaymentId());
                    log.info("외부 결제사 상태 동기화: paymentId={}, {} -> {}", 
                            payment.getId(), currentStatus, latestStatus);
                } else {
                    log.warn("상태 전이 조건 불일치로 스킵: paymentId={}, 현재 상태={}, 외부 상태={}", 
                            payment.getId(), currentStatus, latestStatus);
                }
                break;
                
            case FAILED:
            case ABORTED:
            case EXPIRED:
                if (currentStatus.isPending()) {
                    payment.fail(result.getFailureReason() != null ? result.getFailureReason() : "외부 결제사에서 결제 실패로 확인됨");
                    log.info("외부 결제사 상태 동기화: paymentId={}, {} -> {}", 
                            payment.getId(), currentStatus, latestStatus);
                } else {
                    log.warn("상태 전이 조건 불일치로 스킵: paymentId={}, 현재 상태={}, 외부 상태={}", 
                            payment.getId(), currentStatus, latestStatus);
                }
                break;
                
            case CANCELED:
                if (currentStatus == PaymentStatus.SUCCESS || currentStatus.isPending()) {
                    payment.cancel();
                    log.info("외부 결제사 상태 동기화: paymentId={}, {} -> {}", 
                            payment.getId(), currentStatus, latestStatus);
                } else {
                    log.warn("상태 전이 조건 불일치로 스킵: paymentId={}, 현재 상태={}, 외부 상태={}", 
                            payment.getId(), currentStatus, latestStatus);
                }
                break;
                
            case READY:
            case IN_PROGRESS:
            case WAITING_FOR_DEPOSIT:
                if (currentStatus.isPending()) {
                    payment.updateStatus(latestStatus);
                    log.info("외부 결제사 상태 동기화: paymentId={}, {} -> {}", 
                            payment.getId(), currentStatus, latestStatus);
                } else {
                    log.warn("상태 전이 조건 불일치로 스킵: paymentId={}, 현재 상태={}, 외부 상태={}", 
                            payment.getId(), currentStatus, latestStatus);
                }
                break;
                
            case REFUNDED:
            case PARTIALLY_REFUNDED:
                log.warn("환불 상태 동기화는 별도 프로세스에서 처리됨: paymentId={}, 현재 상태={}, 외부 상태={}", 
                        payment.getId(), currentStatus, latestStatus);
                break;
                
            default:
                log.warn("알 수 없는 결제 상태: paymentId={}, 상태={}", payment.getId(), latestStatus);
                break;
        }
    }
}

