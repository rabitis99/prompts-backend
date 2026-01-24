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
     * @ModelAttribute 바인딩을 위한 snake_case 파라미터 지원
     *
     * 예)
     * - ?user_id=1
     * - ?client_ip=192.168.1.1
     * - ?rule_name=api_rate_limit
     * - ?rate_limit_type=API
     * - ?start_date=2024-01-01T00:00:00
     * - ?end_date=2024-01-31T23:59:59
     */
    /**
     * 서비스/테스트 코드에서 사용하기 좋은 정적 팩토리 메서드
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

