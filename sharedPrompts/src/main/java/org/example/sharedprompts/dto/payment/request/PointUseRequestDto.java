package org.example.sharedprompts.dto.payment.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.Point;
import org.example.sharedprompts.domain.user.User;

import java.math.BigDecimal;

/**
 * 포인트 사용 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointUseRequestDto {

    @NotNull(message = "사용할 포인트를 입력해주세요.")
    @DecimalMin(value = "1", message = "포인트는 1 이상이어야 합니다.")
    private BigDecimal amount;

    @NotBlank(message = "사용 사유를 입력해주세요.")
    private String description;

    /**
     * Create a Point builder preconfigured for a point usage transaction.
     *
     * @param user    the owner of the point entry
     * @param balance the resulting balance to set on the Point
     * @return        a Point.PointBuilder initialized for a usage transaction with the amount stored as negative, type set to "USE", the provided description and balance, and expired set to false
     */
    public Point.PointBuilder toPointBuilder(User user, BigDecimal balance) {
        return Point.builder()
                .user(user)
                .amount(this.amount.negate()) // 음수로 저장 (사용)
                .type("USE")
                .description(this.description)
                .balance(balance)
                .expired(false);
    }
}
