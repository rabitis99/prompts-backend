package org.example.sharedprompts.domain.payment.repository;

import org.example.sharedprompts.domain.payment.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 환율 Repository
 */
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * Finds the exchange rate for a given currency pair.
     *
     * @param fromCurrency the source currency code (e.g., "USD")
     * @param toCurrency   the target currency code (e.g., "KRW")
     * @return             an Optional containing the matching ExchangeRate, or empty if none exists
     */
    @Query("SELECT e FROM ExchangeRate e WHERE e.fromCurrency = :fromCurrency AND e.toCurrency = :toCurrency")
    Optional<ExchangeRate> findByCurrencyPair(
            @Param("fromCurrency") String fromCurrency,
            @Param("toCurrency") String toCurrency
    );

    /**
 * Checks whether an exchange rate exists for the specified currency pair.
 *
 * @param fromCurrency the source currency code (e.g., "USD")
 * @param toCurrency the target currency code (e.g., "KRW")
 * @return `true` if an ExchangeRate for the given currency pair exists, `false` otherwise
 */
    boolean existsByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
}
