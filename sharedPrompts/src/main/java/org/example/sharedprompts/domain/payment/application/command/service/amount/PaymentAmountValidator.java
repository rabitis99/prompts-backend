package org.example.sharedprompts.domain.payment.application.command.service.amount;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class PaymentAmountValidator {

    public void validatePaymentInputs(Long userId, BigDecimal originalAmount, String currencyCode) {
        validateUserId(userId);
        validateAmount(originalAmount);
        validateCurrencyCode(currencyCode);
    }

    public void validateAmount(BigDecimal amount) {
        if (amount == null) {
            log.warn("결제 금액이 null입니다");
            throw new ApiException(ErrorCode.PAYMENT_AMOUNT_INVALID, "결제 금액은 필수입니다");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("결제 금액이 0 이하입니다: amount={}", amount);
            throw new ApiException(ErrorCode.PAYMENT_AMOUNT_INVALID, "결제 금액은 0보다 커야 합니다");
        }
    }

    public void validateUserId(Long userId) {
        if (userId == null) {
            log.warn("userId가 null입니다");
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "userId는 필수입니다");
        }
    }

    public void validateCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            log.warn("통화 코드가 null이거나 비어있습니다");
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "통화 코드는 필수입니다");
        }
    }
}

