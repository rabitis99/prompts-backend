package org.example.sharedprompts.dto.payment.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.enums.PaymentUserType;
import org.example.sharedprompts.domain.payment.enums.UserTier;
import org.example.sharedprompts.domain.user.User;

import java.math.BigDecimal;

/**
 * 결제 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    @NotNull(message = "결제 금액을 입력해주세요.")
    @DecimalMin(value = "0.01", message = "결제 금액은 0.01 이상이어야 합니다.")
    private BigDecimal amount;

    @NotBlank(message = "통화를 입력해주세요.")
    @JsonProperty("currency")
    private String currency; // ISO 4217 통화 코드 (KRW, USD, EUR 등)

    @NotNull(message = "결제 수단을 선택해주세요.")
    @JsonProperty("payment_method")
    private PaymentMethod paymentMethod;

    @NotNull(message = "사용자 타입을 선택해주세요.")
    @JsonProperty("user_type")
    private PaymentUserType userType;

    @JsonProperty("use_point_amount")
    @DecimalMin(value = "0", message = "사용할 포인트는 0 이상이어야 합니다.")
    private BigDecimal usePointAmount; // 사용할 포인트 금액 (선택사항)

    @JsonProperty("metadata")
    private String metadata; // 추가 메타데이터 (JSON 형태)

    /**
     * Create a Payment builder pre-populated from this DTO and the provided values.
     *
     * @param user the owner of the payment
     * @param tier the user's tier to associate with the payment
     * @param convertedAmount the payment amount converted to the target currency
     * @param usedPointAmount the amount of points to apply to the payment
     * @return a Payment.PaymentBuilder preconfigured with user, amount, currency, paymentMethod, userType, tier, status set to {@code PaymentStatus.PENDING}, usedPointAmount, and metadata
     */
    public Payment.PaymentBuilder toPaymentBuilder(User user, UserTier tier, BigDecimal convertedAmount, BigDecimal usedPointAmount) {
        return Payment.builder()
                .user(user)
                .amount(convertedAmount)
                .currency(this.currency)
                .paymentMethod(this.paymentMethod)
                .userType(this.userType)
                .tier(tier)
                .status(PaymentStatus.PENDING)
                .usedPointAmount(usedPointAmount)
                .metadata(this.metadata);
    }
}
