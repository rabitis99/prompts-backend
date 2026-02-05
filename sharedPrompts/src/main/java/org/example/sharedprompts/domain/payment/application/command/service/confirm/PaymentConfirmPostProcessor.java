package org.example.sharedprompts.domain.payment.application.command.service.confirm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.orchestrator.CompensationHandler;
import org.example.sharedprompts.domain.payment.application.command.postprocess.PaymentPostProcessService;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.compensation.CompensationTaskType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConfirmPostProcessor {

    private final PaymentPostProcessService postProcessService;
    private final CompensationHandler compensationHandler;

    public void processSuccessPostCommit(Payment payment, Long userId, long processingTime,
                                       BigDecimal actualAmount, BigDecimal originalAmount) {
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            log.warn("결제 성공 후처리 스킵: 결제 상태가 SUCCESS가 아님. paymentId={}, status={}", 
                    payment.getId(), payment.getStatus());
            return;
        }

        try {
            postProcessService.processPaymentSuccessAfterCommit(
                    payment.getId(),
                    userId,
                    actualAmount,
                    originalAmount,
                    processingTime
            );
        } catch (Exception postProcessException) {
            log.error("결제 승인 성공 후 후처리 실패: paymentId={}, userId={}, error={}",
                    payment.getId(), userId, postProcessException.getMessage(), postProcessException);

            compensationHandler.handlePostProcessFailure(
                    CompensationTaskType.POINT_ACCRUAL,
                    payment.getId(),
                    userId,
                    actualAmount,
                    postProcessException.getMessage()
            );
        }
    }
}

