package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 리워드 설정 Properties (Immutable)
 * - 캐시백 및 포인트 적립률 관리
 */
@Getter
@Component
public class RewardProperties {

    private final double cashbackRate;
    private final double pointRate;

    public RewardProperties(
            @Value("${payment.cashback.rate:0.01}") double cashbackRate,
            @Value("${payment.point.rate:0.005}") double pointRate) {
        this.cashbackRate = cashbackRate;
        this.pointRate = pointRate;
    }
}

