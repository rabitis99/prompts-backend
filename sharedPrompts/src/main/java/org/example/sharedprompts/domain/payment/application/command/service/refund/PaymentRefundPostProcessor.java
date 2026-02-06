package org.example.sharedprompts.domain.payment.application.command.service.refund;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.orchestrator.CompensationHandler;
import org.example.sharedprompts.domain.payment.application.command.postprocess.PaymentPostProcessService;
import org.example.sharedprompts.domain.payment.application.command.service.amount.PaymentAmountProcessingService;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.compensation.CompensationTaskType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundPostProcessor {

    private final PaymentAmountProcessingService amountProcessingService;
    private final PaymentPostProcessService postProcessService;
    private final CompensationHandler compensationHandler;

    public void processAfterCommit(Payment refundedPayment, Long userId,
                                  BigDecimal refundAmount, String reason) {
        if (refundedPayment == null || refundedPayment.getId() == null || refundAmount == null) {
            log.error("결제 환불 후처리 스킵: refundedPayment 또는 refundAmount가 유효하지 않음");
            return;
        }

        BigDecimal refundPointAmount = amountProcessingService.calculateRefundPointAmount(
                refundedPayment.getUsedPointAmount(),
                refundedPayment.getAmount(),
                refundAmount
        );

        try {
            postProcessService.processPaymentRefundAfterCommit(
                    refundedPayment.getId(),
                    userId,
                    refundAmount,
                    refundPointAmount,
                    reason
            );
            log.debug("결제 환불 후처리 완료: paymentId={}, userId={}",
                    refundedPayment.getId(), userId);
        } catch (Exception postProcessException) {
            log.error("결제 환불 성공 후 후처리 실패: paymentId={}, userId={}, error={}",
                    refundedPayment.getId(), userId, postProcessException.getMessage(), postProcessException);

            try {
                compensationHandler.handlePostProcessFailure(
                        CompensationTaskType.POINT_RECOVERY_REFUND,
                        refundedPayment.getId(),
                        userId,
                        refundPointAmount,
                        postProcessException.getMessage()
                );
                log.info("결제 환불 후처리 실패에 대한 보상 처리 완료: paymentId={}",
                        refundedPayment.getId());
            } catch (Exception compensationException) {
                log.error("결제 환불 후처리 실패에 대한 보상 처리도 실패: paymentId={}, userId={}, error={}",
                        refundedPayment.getId(), userId, compensationException.getMessage(), compensationException);
            }
        }
    }
}

