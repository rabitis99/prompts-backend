package org.example.sharedprompts.dto.statistics.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 개인 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyStatisticsResponseDto {

    /**
     * 내가 작성한 프롬프트 수
     */
    @JsonProperty("my_prompts_count")
    private Long myPromptsCount;

    /**
     * 내 프롬프트에 받은 총 좋아요 수
     */
    @JsonProperty("total_likes_received")
    private Long totalLikesReceived;
}

