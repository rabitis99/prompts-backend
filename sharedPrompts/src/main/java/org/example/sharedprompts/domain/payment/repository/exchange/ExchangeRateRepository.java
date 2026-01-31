package org.example.sharedprompts.domain.payment.repository.exchange;

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
     * 통화 쌍으로 환율 조회
     */
    @Query("SELECT e FROM ExchangeRate e WHERE e.fromCurrency = :fromCurrency AND e.toCurrency = :toCurrency")
    Optional<ExchangeRate> findByCurrencyPair(
            @Param("fromCurrency") String fromCurrency,
            @Param("toCurrency") String toCurrency
    );
}

