package org.example.sharedprompts.domain.payment.service.cashback.amount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 캐시백 금액 계산 서비스
 * 
 * <p>단일 책임: 캐시백 금액 계산만 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashbackAmountService {

    /**
     * 캐시백 금액 계산
     * 
     * @param paymentAmount 결제 금액
     * @param cashbackRate 캐시백 비율 (예: 0.05 = 5%)
     * @return 계산된 캐시백 금액 (소수점 둘째 자리까지)
     */
    public BigDecimal calculateCashbackAmount(BigDecimal paymentAmount, BigDecimal cashbackRate) {
        return paymentAmount.multiply(cashbackRate)
                .setScale(2, RoundingMode.DOWN); // 소수점 둘째 자리까지
    }
}

