package org.example.sharedprompts.domain.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 환율 API 설정 Properties (Immutable)
 */
@Getter
@Component
public class ExchangeRateProperties {

    private final String apiKey;
    private final String apiUrl;

    public ExchangeRateProperties(
            @Value("${payment.exchange-rate.api-key:}") String apiKey,
            @Value("${payment.exchange-rate.api-url:https://api.exchangerate-api.com/v4/latest/}") String apiUrl) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }
}


