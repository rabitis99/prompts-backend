package org.example.sharedprompts.domain.like;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.entity.BaseEntity;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "comment_likes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_comment_like_comment_user",
                        columnNames = {"comment_id", "user_id"}
                )
        },
        indexes = {
                // 추가된 인덱스 목록 (우선순위: 권장)
                // 사용자별 좋아요 이력 조회 시 정렬 최적화
                @Index(name = "idx_comment_likes_user_created_at", columnList = "user_id, created_at"),
                // 댓글별 좋아요 조회 시 정렬 최적화
                @Index(name = "idx_comment_likes_comment_created_at", columnList = "comment_id, created_at")
        }
)
public class CommentLike extends BaseEntity {

    @EmbeddedId
    private CommentLikeId id;

    @MapsId("commentId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

}
