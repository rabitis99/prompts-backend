package org.example.sharedprompts.domain.payment.application.command.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.postprocess.PaymentPostProcessService;
import org.example.sharedprompts.domain.payment.application.command.service.amount.PaymentAmountProcessingService;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.event.PaymentConfirmedEvent;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

/**
 * 결제 확인 도메인 이벤트({@link PaymentConfirmedEvent})를 구독하여
 * 포인트 적립, 캐시백 적립, 유저 티어 업그레이드를 수행합니다.
 *
 * <p>POST /payments/confirm → UseCase(DefaultPaymentConfirmationService) 경로에서는
 * 기존 레거시 {@link org.example.sharedprompts.domain.payment.application.command.service.PaymentConfirmService}의
 * 후처리가 호출되지 않으므로, 이 리스너가 동일한 후처리를 트리거합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConfirmedRewardListener {

    private final PaymentJpaAdapter paymentJpaAdapter;
    private final PaymentAmountProcessingService amountProcessingService;
    private final PaymentPostProcessService postProcessService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        Long paymentId = event.getPaymentId();
        Long userId = event.getUserId();
        BigDecimal originalAmount = event.getAmount();

        var paymentOpt = paymentJpaAdapter.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            log.warn("결제 확인 보상 후처리 스킵: 결제 없음. paymentId={}", paymentId);
            return;
        }

        var payment = paymentOpt.get();
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            log.warn("결제 확인 보상 후처리 스킵: 상태가 SUCCESS가 아님. paymentId={}, status={}",
                    paymentId, payment.getStatus());
            return;
        }

        BigDecimal actualAmount = amountProcessingService.calculateActualAmount(
                payment.getAmount(),
                payment.getUsedPointAmount() != null ? payment.getUsedPointAmount() : BigDecimal.ZERO
        );

        try {
            postProcessService.processPaymentSuccessAfterCommit(
                    paymentId,
                    userId,
                    actualAmount,
                    originalAmount,
                    0L
            );
            log.info("결제 확인 보상 후처리 완료: paymentId={}, userId={}", paymentId, userId);
        } catch (Exception e) {
            log.error("결제 확인 보상 후처리 실패: paymentId={}, userId={}, error={}",
                    paymentId, userId, e.getMessage(), e);
            throw e;
        }
    }
}
