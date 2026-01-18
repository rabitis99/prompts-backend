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
@Table(name = "comment_likes")
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
