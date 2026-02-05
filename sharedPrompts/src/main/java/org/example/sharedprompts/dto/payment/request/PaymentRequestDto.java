package org.example.sharedprompts.dto.payment.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentUserType;
import org.example.sharedprompts.domain.user.User;

import java.math.BigDecimal;

/**
 * 결제 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

    @NotNull(message = "결제 금액을 입력해주세요.")
    @DecimalMin(value = "0.01", message = "결제 금액은 0.01 이상이어야 합니다.")
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank(message = "통화를 입력해주세요.")
    @JsonProperty("currency")
    private String currency; // ISO 4217 통화 코드 (KRW, USD, EUR 등)

    @NotNull(message = "결제 수단을 선택해주세요.")
    @JsonProperty("payment_method")
    private PaymentMethod paymentMethod;

    @JsonProperty("use_point_amount")
    @DecimalMin(value = "0", message = "사용할 포인트는 0 이상이어야 합니다.")
    private BigDecimal usePointAmount; // 사용할 포인트 금액 (선택사항)

    @JsonProperty("metadata")
    private String metadata; // 추가 메타데이터 (JSON 형태)

    /**
     * Payment 엔티티 빌더 생성
     * 서버에서 사용자의 현재 정보를 사용하여 Payment 엔티티를 생성합니다.
     */
    public Payment.PaymentBuilder toPaymentBuilder(User user, BigDecimal convertedAmount, BigDecimal usedPointAmount) {
        return Payment.builder()
                .user(user)
                .amount(convertedAmount)
                .currency(this.currency)
                .paymentMethod(this.paymentMethod)
                .status(PaymentStatus.PENDING)
                .usedPointAmount(usedPointAmount)
                .metadata(this.metadata)
                .userType(determineUserType(user))
                .tier(user.getTier());
    }

    private PaymentUserType determineUserType(User user) {
        return PaymentUserType.PERSONAL;
    }
}

