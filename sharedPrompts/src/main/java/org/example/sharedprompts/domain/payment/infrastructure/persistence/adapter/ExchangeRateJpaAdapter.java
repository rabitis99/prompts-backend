package org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.entity.ExchangeRate;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.exchange.ExchangeRateRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExchangeRateJpaAdapter {

    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRate save(ExchangeRate exchangeRate) {
        return exchangeRateRepository.save(exchangeRate);
    }

    public Optional<ExchangeRate> findById(Long id) {
        return exchangeRateRepository.findById(id);
    }

    public Optional<ExchangeRate> findByCurrencyPair(String fromCurrency, String toCurrency) {
        return exchangeRateRepository.findByCurrencyPair(fromCurrency, toCurrency);
    }
}





