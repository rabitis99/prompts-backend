package org.example.sharedprompts.domain.comment;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@Table(
        name = "comments",
        indexes = {
                // 프롬프트 상세 페이지에서 루트 댓글을 프롬프트/작성일 기준으로 조회할 때 사용
                @Index(name = "idx_comments_prompt_created_at", columnList = "prompt_id, created_at"),
                // 대댓글 조회 및 정렬을 위한 인덱스
                @Index(name = "idx_comments_parent_created_at", columnList = "parent_id, created_at"),
                // 특정 사용자의 댓글 이력 조회(마이페이지 등)를 고려한 인덱스
                @Index(name = "idx_comments_user_created_at", columnList = "user_id, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Comment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id", nullable = false)
    private Prompt prompt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<Comment> children = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private long replyCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private long likeCount = 0L;

    public void updateContent(String content) {
        this.content = content;
    }

}
