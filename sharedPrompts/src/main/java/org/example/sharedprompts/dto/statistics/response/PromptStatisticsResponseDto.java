package org.example.sharedprompts.dto.statistics.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 프롬프트 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptStatisticsResponseDto {

    /**
     * 전체 프롬프트 수
     */
    @JsonProperty("total_prompts")
    private Long totalPrompts;

    /**
     * 오늘 생성된 프롬프트 수
     */
    @JsonProperty("today_created_prompts")
    private Long todayCreatedPrompts;

    /**
     * 최근 7일 생성된 프롬프트 수
     */
    @JsonProperty("weekly_created_prompts")
    private Long weeklyCreatedPrompts;

    /**
     * 최근 30일 생성된 프롬프트 수
     */
    @JsonProperty("monthly_created_prompts")
    private Long monthlyCreatedPrompts;

    /**
     * 전체 조회 수
     */
    @JsonProperty("total_view_count")
    private Long totalViewCount;

    /**
     * 전체 좋아요 수
     */
    @JsonProperty("total_like_count")
    private Long totalLikeCount;

    /**
     * 인기 태그 목록 (상위 10개)
     */
    @JsonProperty("popular_tags")
    private List<PopularTagDto> popularTags;
}
