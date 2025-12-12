package org.example.sharedprompts.domain.like.event;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LikeEvent {
    // 프롬프트 좋아요 추가
    public record PromptLiked(Long promptId) {}

    // 프롬프트 좋아요 취소
    public record PromptUnliked(Long promptId) {}

    // 댓글 좋아요 추가
    public record CommentLiked(Long commentId) {}

    // 댓글 좋아요 취소
    public record CommentUnliked(Long commentId) {}
}
