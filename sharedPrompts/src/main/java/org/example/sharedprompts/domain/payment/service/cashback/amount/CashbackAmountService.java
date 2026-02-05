package org.example.sharedprompts.domain.payment.service.cashback.amount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.valueobject.PaymentAmount;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 캐시백 금액 계산 서비스
 * 
 * <p>단일 책임: 캐시백 금액 계산만 담당
 * ValueObject를 사용하여 타입 안정성과 도메인 규칙을 보장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashbackAmountService {

    /**
     * 캐시백 금액 계산
     * 
     * <p>ValueObject를 사용하여 금액 계산의 타입 안정성을 보장합니다.
     * 
     * @param paymentAmount 결제 금액
     * @param cashbackRate 캐시백 비율 (예: 0.05 = 5%)
     * @return 계산된 캐시백 금액 (소수점 둘째 자리까지)
     */
    public BigDecimal calculateCashbackAmount(BigDecimal paymentAmount, BigDecimal cashbackRate) {
        // PaymentAmount로 변환 (캐시백은 KRW 기준)
        PaymentAmount payment = PaymentAmount.krw(paymentAmount);
        
        // 캐시백 금액 계산
        PaymentAmount cashbackAmount = payment.multiply(cashbackRate);
        
        // 소수점 둘째 자리까지 반올림하여 반환
        return cashbackAmount.toBigDecimal().setScale(2, RoundingMode.DOWN);
    }
    
    /**
     * 캐시백 금액 계산 (통화 정보 포함)
     * 
     * <p>통화 정보가 있는 경우 사용합니다.
     * 
     * @param paymentAmount 결제 금액
     * @param currency 통화 코드
     * @param cashbackRate 캐시백 비율 (예: 0.05 = 5%)
     * @return 계산된 캐시백 금액 (소수점 둘째 자리까지)
     */
    public BigDecimal calculateCashbackAmount(BigDecimal paymentAmount, String currency, BigDecimal cashbackRate) {
        // PaymentAmount로 변환
        PaymentAmount payment = PaymentAmount.of(paymentAmount, currency);
        
        // 캐시백 금액 계산
        PaymentAmount cashbackAmount = payment.multiply(cashbackRate);
        
        // 소수점 둘째 자리까지 반올림하여 반환
        return cashbackAmount.toBigDecimal().setScale(2, RoundingMode.DOWN);
    }
}

