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
@Table(name = "prompts")
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

    @Column(nullable = false)
    @Builder.Default
    private long commentCount = 0L;

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

    public void updateViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
}
