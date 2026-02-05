package org.example.sharedprompts.domain.payment.application.command.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.application.command.service.provider.PaymentPreparationResultHandler;
import org.example.sharedprompts.domain.payment.application.command.service.provider.PaymentPreparationService;
import org.example.sharedprompts.domain.payment.application.command.service.provider.PreparePaymentResult;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProviderIntegrationService {

    private final PaymentPreparationService preparationService;
    private final PaymentPreparationResultHandler resultHandler;

    public PreparePaymentResult preparePayment(Payment payment, BigDecimal actualAmount,
                                              String productName, Long userId) {
        validateInputs(payment, actualAmount, userId);

        log.info("결제 준비 시작: paymentId={}, paymentMethod={}, userId={}",
                payment.getId(), payment.getPaymentMethod(), userId);

        try {
            var prepareResult = preparationService.preparePayment(payment, actualAmount, productName, userId);
            return resultHandler.handlePreparationResult(payment, prepareResult);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("결제 준비 실패 (비즈니스 로직 오류): paymentId={}, paymentMethod={}, error={}",
                    payment.getId(), payment.getPaymentMethod(), e.getMessage(), e);
            throw e;
        } catch (RuntimeException e) {
            log.error("결제 준비 실패 (런타임 오류): paymentId={}, paymentMethod={}, error={}",
                    payment.getId(), payment.getPaymentMethod(), e.getMessage(), e);
            throw new PaymentPreparationException("결제 준비 중 오류가 발생했습니다", e);
        } catch (Exception e) {
            log.error("결제 준비 실패 (예상치 못한 오류): paymentId={}, paymentMethod={}, error={}",
                    payment.getId(), payment.getPaymentMethod(), e.getMessage(), e);
            throw new PaymentPreparationException("결제 준비 중 예상치 못한 오류가 발생했습니다", e);
        }
    }

    private void validateInputs(Payment payment, BigDecimal actualAmount, Long userId) {
        if (payment == null) {
            throw new IllegalArgumentException("payment는 필수입니다");
        }
        if (actualAmount == null || actualAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("actualAmount는 0보다 커야 합니다");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다");
        }
    }

    public static class PaymentPreparationException extends RuntimeException {
        public PaymentPreparationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

