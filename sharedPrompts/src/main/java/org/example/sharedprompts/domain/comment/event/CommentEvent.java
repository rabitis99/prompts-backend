package org.example.sharedprompts.domain.comment.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommentEvent {

    // 댓글 생성 이벤트
    public record Created(Long commentId, Long userId, Long promptId, Long parentId) {}

    // 댓글 삭제 이벤트
    public record Deleted(Long promptId, Long parentId) {}
}