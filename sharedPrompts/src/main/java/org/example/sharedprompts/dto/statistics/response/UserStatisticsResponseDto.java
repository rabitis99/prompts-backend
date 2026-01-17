package org.example.sharedprompts.dto.statistics.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 사용자 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsResponseDto {

    /**
     * 전체 가입자 수
     */
    @JsonProperty("total_users")
    private Long totalUsers;

    /**
     * 활성 사용자 수 (최근 30일 내 활동)
     */
    @JsonProperty("active_users")
    private Long activeUsers;

    /**
     * 오늘 신규 가입자 수
     */
    @JsonProperty("today_new_users")
    private Long todayNewUsers;

    /**
     * 최근 7일 신규 가입자 수
     */
    @JsonProperty("weekly_new_users")
    private Long weeklyNewUsers;

    /**
     * 최근 30일 신규 가입자 수
     */
    @JsonProperty("monthly_new_users")
    private Long monthlyNewUsers;

    /**
     * 일별 신규 가입자 추이 (최근 30일)
     */
    @JsonProperty("daily_new_users_trend")
    private List<DailyNewUsersTrendDto> dailyNewUsersTrend;
}
