package org.example.sharedprompts.dto.payment.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.payment.enums.UserTier;

/**
 * 티어 변경 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierChangeRequestDto {

    @NotNull(message = "티어를 선택해주세요.")
    @JsonProperty("tier")
    private UserTier tier;

    private String reason; // 변경 사유

    /**
     * 변경 사유 반환 (null인 경우 기본값)
     */
    public String getReasonOrDefault() {
        return this.reason != null ? this.reason : "티어 변경";
    }
}

