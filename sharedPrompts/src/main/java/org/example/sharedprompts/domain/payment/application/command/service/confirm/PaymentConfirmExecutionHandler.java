package org.example.sharedprompts.domain.payment.application.command.service.confirm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.PaymentExecutionService;
import org.example.sharedprompts.domain.payment.application.command.postprocess.PaymentPostProcessService;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.infrastructure.monitoring.PaymentLoggingService;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConfirmExecutionHandler {

    private final PaymentExecutionService executionService;
    private final PaymentPostProcessService postProcessService;
    private final PaymentLoggingService loggingService;
    private final PaymentJpaAdapter paymentJpaAdapter;

    public PaymentExecutionResult executePayment(Payment payment, BigDecimal actualAmount,
                                                BigDecimal originalAmount,
                                                Map<String, String> additionalParams,
                                                Long userId, long startTime) {
        try {
            payment = executionService.executePayment(payment, actualAmount, additionalParams);

            long processingTime = System.currentTimeMillis() - startTime;
            boolean succeeded = payment.getStatus() == PaymentStatus.SUCCESS;

            if (succeeded) {
                loggingService.logPaymentApprovalSuccess(payment, payment.getExternalPaymentId(), processingTime);
                loggingService.logPaymentStatusChange(payment, PaymentStatus.PENDING, PaymentStatus.SUCCESS);
            } else {
                log.warn("결제 승인 후 상태가 SUCCESS가 아님: paymentId={}, status={}", 
                        payment.getId(), payment.getStatus());
            }

            return new PaymentExecutionResult(payment, processingTime, succeeded, actualAmount, originalAmount);

        } catch (Exception e) {
            return handlePaymentExecutionFailure(payment, userId, e, startTime);
        }
    }

    private PaymentExecutionResult handlePaymentExecutionFailure(Payment payment, Long userId,
                                                                 Exception e, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        payment.fail("결제 승인 실패: " + e.getMessage());
        payment = paymentJpaAdapter.save(payment);

        try {
            postProcessService.processPaymentFailure(
                    payment,
                    userId,
                    e.getMessage(),
                    e,
                    processingTime
            );
        } catch (Exception postProcessException) {
            log.error("결제 실패 후처리 중 오류 발생: paymentId={}, userId={}, error={}",
                    payment.getId(), userId, postProcessException.getMessage(), postProcessException);
        }

        log.error("결제 승인 실패: paymentId={}, userId={}, error={}", 
                payment.getId(), userId, e.getMessage(), e);
        throw new ApiException(ErrorCode.PAYMENT_PROVIDER_ERROR, "결제 승인 실패: " + e.getMessage());
    }
}

