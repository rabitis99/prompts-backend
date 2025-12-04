package org.example.sharedprompts.domain.prompt;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.entity.BaseEntity;

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
    private boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    private PromptCategory promptCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false)
    @Builder.Default
    private int viewCount = 0;

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateStatus(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void updateViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
}
