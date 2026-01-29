package org.example.sharedprompts.domain.payment;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환율 엔티티
 * 국가별 환율 정보를 저장
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "exchange_rates",
        indexes = {
                @Index(name = "idx_exchange_rates_from_to", columnList = "from_currency, to_currency", unique = true),
                @Index(name = "idx_exchange_rates_updated_at", columnList = "updated_at")
        }
)
public class ExchangeRate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String fromCurrency; // 기준 통화 (ISO 4217 코드, 예: USD)

    @Column(nullable = false, length = 3)
    private String toCurrency; // 대상 통화 (ISO 4217 코드, 예: KRW)

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rate; // 환율 (1 fromCurrency = rate toCurrency)

    @Column(nullable = false)
    private LocalDateTime lastFetchedAt; // 마지막 환율 API 호출 시간

    /**
     * Update the stored exchange rate and the timestamp when it was fetched.
     *
     * @param newRate  the new exchange rate (how many units of toCurrency equal one unit of fromCurrency)
     * @param fetchedAt the timestamp when the rate was retrieved
     */
    public void updateRate(BigDecimal newRate, LocalDateTime fetchedAt) {
        this.rate = newRate;
        this.lastFetchedAt = fetchedAt;
    }
}
