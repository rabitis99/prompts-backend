package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 리워드 설정 Properties (Immutable)
 * - 캐시백 및 포인트 적립률 관리
 */
@Getter
@Component
public class RewardProperties {

    private final BigDecimal cashbackRate;
    private final BigDecimal pointRate;

    public RewardProperties(
            @Value("${payment.cashback.rate:0.01}") BigDecimal cashbackRate,
            @Value("${payment.point.rate:0.005}") BigDecimal pointRate) {
        this.cashbackRate = cashbackRate;
        this.pointRate = pointRate;
    }
}











