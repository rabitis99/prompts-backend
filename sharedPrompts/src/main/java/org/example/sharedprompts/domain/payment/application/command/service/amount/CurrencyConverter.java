package org.example.sharedprompts.domain.payment.application.command.service.amount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.service.PaymentAmountCalculator;
import org.example.sharedprompts.domain.payment.domain.valueobject.Currency;
import org.example.sharedprompts.domain.payment.domain.valueobject.ExchangeRate;
import org.example.sharedprompts.domain.payment.domain.valueobject.PaymentAmount;
import org.example.sharedprompts.domain.payment.infrastructure.external.exchange.ExchangeRateService;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyConverter {

    private static final Currency KRW = Currency.KRW();
    
    private final PaymentAmountCalculator amountCalculator;
    private final ExchangeRateService exchangeRateService;

    public PaymentAmount convertToKrw(PaymentAmount originalAmount) {
        if (originalAmount.getCurrency().equals(KRW)) {
            return originalAmount;
        }

        try {
            BigDecimal exchangeRate = fetchExchangeRate(originalAmount);
            ExchangeRate rate = createExchangeRate(originalAmount, exchangeRate);
            PaymentAmount converted = amountCalculator.convertCurrency(originalAmount, rate);
            
            log.info("환율 변환 완료: {} -> {} (환율: {})", 
                    originalAmount, converted, exchangeRate);
            return converted;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("환율 변환 실패: currency={}, amount={}", 
                    originalAmount.getCurrencyCode(), originalAmount, e);
            throw new ApiException(ErrorCode.EXCHANGE_RATE_ERROR, "환율 변환에 실패했습니다", e);
        }
    }

    private BigDecimal fetchExchangeRate(PaymentAmount originalAmount) {
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
                originalAmount.getCurrencyCode(),
                KRW.getCode()
        );

        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("유효하지 않은 환율: from={}, to=KRW, rate={}", 
                    originalAmount.getCurrencyCode(), exchangeRate);
            throw new ApiException(ErrorCode.EXCHANGE_RATE_ERROR, "유효하지 않은 환율입니다");
        }

        return exchangeRate;
    }

    private ExchangeRate createExchangeRate(PaymentAmount originalAmount, BigDecimal exchangeRate) {
        return ExchangeRate.of(
                originalAmount.getCurrency(),
                KRW,
                exchangeRate
        );
    }
}

