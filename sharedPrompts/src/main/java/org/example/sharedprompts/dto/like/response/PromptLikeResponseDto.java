package org.example.sharedprompts.dto.like.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptLikeResponseDto {

    @JsonProperty("isLiked")
    private Boolean isLiked;

    @JsonProperty("like_count")
    private Long likeCount;

    public static PromptLikeResponseDto from(boolean isLiked) {
        return PromptLikeResponseDto.builder()
                .isLiked(isLiked)
                .build();
    }

    public static PromptLikeResponseDto of(boolean isLiked, Long likeCount) {
        return PromptLikeResponseDto.builder()
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();
    }
}
