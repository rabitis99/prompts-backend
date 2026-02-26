package org.example.sharedprompts.domain.payment.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.dto.response.PaymentResult;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProvider;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.PaymentProviderFactory;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.dto.payment.response.PaymentStatusResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStatusSyncExecutor {

    private static final String LOG_PREFIX = "[PaymentStatusSync] ";

    private final PaymentProviderFactory providerFactory;
    private final PaymentJpaAdapter paymentJpaAdapter;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentStatusResponseDto doSyncPaymentStatus(Payment payment) {
        if (payment == null || payment.getId() == null) {
            throw new ApiException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        Payment managedPayment = paymentJpaAdapter.findById(payment.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        return doSyncInternal(managedPayment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentStatusResponseDto doSyncPaymentStatusById(Long paymentId) {
        if (paymentId == null) {
            throw new ApiException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        Payment payment = paymentJpaAdapter.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        return doSyncInternal(payment);
    }

    private PaymentStatusResponseDto doSyncInternal(Payment payment) {
        String externalPaymentId = payment.getExternalPaymentId();
        if (externalPaymentId == null || externalPaymentId.isBlank()) {
            log.warn(LOG_PREFIX + "externalPaymentId가 없어 상태 동기화를 스킵합니다: paymentId={}", payment.getId());
            return PaymentStatusResponseDto.from(payment);
        }
        PaymentProvider provider = providerFactory.getProvider(payment.getPaymentMethod());
        PaymentResult result = provider.getPaymentStatus(externalPaymentId);
        if (result == null || result.getStatus() == null) {
            log.warn(LOG_PREFIX + "외부 상태 조회 결과가 유효하지 않아 동기화를 스킵합니다: paymentId={}, externalPaymentId={}",
                    payment.getId(), externalPaymentId);
            return PaymentStatusResponseDto.from(payment);
        }

        if (payment.getStatus() != result.getStatus()) {
            PaymentStatus before = payment.getStatus();
            syncPaymentStatusFromResult(payment, result);
            if (payment.getStatus() != before) {
                paymentJpaAdapter.save(payment);
            }
        } else {
            log.info(LOG_PREFIX + "동기화 불필요 (상태 동일): paymentId={}, status={}",
                    payment.getId(), payment.getStatus());
        }

        return PaymentStatusResponseDto.from(payment);
    }

    private void syncPaymentStatusFromResult(Payment payment, PaymentResult result) {
        PaymentStatus currentStatus = payment.getStatus();
        PaymentStatus externalStatus = result.getStatus();

        if (currentStatus == externalStatus) {
            return;
        }

        switch (externalStatus) {
            case SUCCESS:
                if (currentStatus.isPending()) {
                    payment.approve(result.getExternalPaymentId());
                    logStatusSynced(payment, currentStatus, externalStatus);
                } else {
                    logTransitionSkipped(payment, currentStatus, externalStatus);
                }
                break;

            case FAILED:
            case ABORTED:
            case EXPIRED:
                if (currentStatus.isPending()) {
                    payment.fail(result.getFailureReason() != null ? result.getFailureReason() : "외부 결제사에서 결제 실패로 확인됨");
                    logStatusSynced(payment, currentStatus, externalStatus);
                } else {
                    logTransitionSkipped(payment, currentStatus, externalStatus);
                }
                break;

            case CANCELED:
                if (currentStatus == PaymentStatus.SUCCESS || currentStatus.isPending()) {
                    payment.cancel();
                    logStatusSynced(payment, currentStatus, externalStatus);
                } else {
                    logTransitionSkipped(payment, currentStatus, externalStatus);
                }
                break;

            case READY:
            case IN_PROGRESS:
            case WAITING_FOR_DEPOSIT:
                if (currentStatus.isPending()) {
                    payment.updateStatus(externalStatus);
                    logStatusSynced(payment, currentStatus, externalStatus);
                } else {
                    logTransitionSkipped(payment, currentStatus, externalStatus);
                }
                break;

            case REFUNDED:
            case PARTIALLY_REFUNDED:
                log.warn(LOG_PREFIX + "환불 상태 동기화는 별도 프로세스에서 처리됨: paymentId={}, currentStatus={}, externalStatus={}",
                        payment.getId(), currentStatus, externalStatus);
                break;

            default:
                log.warn(LOG_PREFIX + "알 수 없는 결제 상태: paymentId={}, externalStatus={}",
                        payment.getId(), externalStatus);
                break;
        }
    }

    private void logStatusSynced(Payment payment, PaymentStatus from, PaymentStatus to) {
        log.info(LOG_PREFIX + "외부 결제사 상태 동기화: paymentId={}, {} -> {}",
                payment.getId(), from, to);
    }

    private void logTransitionSkipped(Payment payment, PaymentStatus current, PaymentStatus external) {
        log.warn(LOG_PREFIX + "상태 전이 조건 불일치로 스킵: paymentId={}, currentStatus={}, externalStatus={}",
                payment.getId(), current, external);
    }
}

