package org.example.sharedprompts.domain.prompt;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.tag.PromptTag;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "prompts",
        indexes = {
                // 작성자 + 생성일 기준 조회/정렬 (내 프롬프트, 사용자 프롬프트 리스트 등)
                @Index(name = "idx_prompts_author_created_at", columnList = "user_id, created_at"),
                // 카테고리 + 생성일 기준 피드/검색
                @Index(name = "idx_prompts_category_created_at", columnList = "prompt_category, created_at"),
                // 제목 검색 최적화를 위한 인덱스 (LIKE 검색 시에도 일부 활용 가능)
                @Index(name = "idx_prompts_title", columnList = "title")
        }
)
public class Prompt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromptCategory promptCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false)
    @Builder.Default
    private long viewCount = 0L;
    // 루트 댓글만 카운트
    @Column(nullable = false)
    @Builder.Default
    private long commentCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private long likeCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private long favoriteCount = 0L;

    @OneToMany(mappedBy = "prompt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PromptTag> promptTags = new ArrayList<>();

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void updateCategory(PromptCategory promptCategory) {
        this.promptCategory = promptCategory;
    }
}
