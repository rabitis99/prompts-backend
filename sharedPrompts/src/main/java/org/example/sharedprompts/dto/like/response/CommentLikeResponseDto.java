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
public class CommentLikeResponseDto {

    @JsonProperty("isLiked")
    private Boolean isLiked;

    @JsonProperty("like_count")
    private Long likeCount;

    public static CommentLikeResponseDto from(boolean isLiked) {
        return CommentLikeResponseDto.builder()
                .isLiked(isLiked)
                .build();
    }

    public static CommentLikeResponseDto of(boolean isLiked, Long likeCount) {
        return CommentLikeResponseDto.builder()
                .isLiked(isLiked)
                .likeCount(likeCount)
                .build();
    }
}
