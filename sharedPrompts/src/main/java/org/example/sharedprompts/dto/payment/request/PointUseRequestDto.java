package org.example.sharedprompts.dto.payment.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.entity.Point;
import org.example.sharedprompts.domain.payment.domain.enums.PointType;
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
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank(message = "사용 사유를 입력해주세요.")
    @JsonProperty("description")
    private String description;

    /**
     * Point 엔티티 빌더 생성 (사용)
     */
    public Point.PointBuilder toPointBuilder(User user, BigDecimal balance) {
        return Point.builder()
                .user(user)
                .amount(this.amount.negate()) // 음수로 저장 (사용)
                .type(PointType.USE)
                .description(this.description)
                .balance(balance)
                .expired(false);
    }
}

