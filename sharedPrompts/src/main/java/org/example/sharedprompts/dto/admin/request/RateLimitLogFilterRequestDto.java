package org.example.sharedprompts.dto.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.rate.ratelimitlog.enums.RateLimitType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Rate Limit 로그 필터 요청 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RateLimitLogFilterRequestDto {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("client_ip")
    private String clientIp;

    @JsonProperty("rule_name")
    private String ruleName;

    @JsonProperty("rate_limit_type")
    private RateLimitType rateLimitType;

    @JsonProperty("start_date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @JsonProperty("end_date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    /**
     * 정적 팩토리 메서드
     */
    public static RateLimitLogFilterRequestDto of(
            Long userId,
            String clientIp,
            String ruleName,
            RateLimitType rateLimitType,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return RateLimitLogFilterRequestDto.builder()
                .userId(userId)
                .clientIp(clientIp)
                .ruleName(ruleName)
                .rateLimitType(rateLimitType)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}

