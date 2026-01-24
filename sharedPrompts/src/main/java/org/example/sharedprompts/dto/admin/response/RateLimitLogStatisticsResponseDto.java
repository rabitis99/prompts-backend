package org.example.sharedprompts.dto.admin.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.rate.ratelimitlog.enums.RateLimitType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Rate Limit 로그 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitLogStatisticsResponseDto {

    /**
     * 시간대별 통계 (0-23시)
     */
    @JsonProperty("hourly_stats")
    private Map<Integer, Long> hourlyStats;

    /**
     * 규칙별 통계
     */
    @JsonProperty("rule_stats")
    private Map<String, Long> ruleStats;

    /**
     * 타입별 통계
     */
    @JsonProperty("type_stats")
    private Map<RateLimitType, Long> typeStats;

    /**
     * 최다 위반 IP 목록
     */
    @JsonProperty("top_violating_ips")
    private List<TopViolatorDto> topViolatingIps;

    /**
     * 최다 위반 사용자 목록
     */
    @JsonProperty("top_violating_users")
    private List<TopViolatorDto> topViolatingUsers;

    /**
     * 통계 기간 시작일
     */
    @JsonProperty("start_date")
    private LocalDateTime startDate;

    /**
     * 통계 기간 종료일
     */
    @JsonProperty("end_date")
    private LocalDateTime endDate;

    /**
     * 위반자 정보 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopViolatorDto {
        /**
         * 식별자 (IP 주소 또는 사용자 ID)
         */
        private String identifier;

        /**
         * 위반 횟수
         */
        @JsonProperty("violation_count")
        private Long violationCount;
    }
}
