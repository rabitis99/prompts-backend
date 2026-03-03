package org.example.sharedprompts.domain.prompt.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.tag.PromptTag;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "task_domain")
    private TaskDomain taskDomain;

    @Enumerated(EnumType.STRING)
    @Column(name = "objective")
    private PromptObjective objective;

    @Enumerated(EnumType.STRING)
    @Column(name = "tone")
    private ToneType tone;

    @Enumerated(EnumType.STRING)
    @Column(name = "style")
    private StyleType style;

    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    private LanguageType language;

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

    public void updateTaskDomain(TaskDomain taskDomain) {
        this.taskDomain = taskDomain;
    }

    public void updateObjective(PromptObjective objective) {
        this.objective = objective;
    }

    public void updateTone(ToneType tone) {
        this.tone = tone;
    }

    public void updateStyle(StyleType style) {
        this.style = style;
    }

    public void updateLanguage(LanguageType language) {
        this.language = language;
    }
}
