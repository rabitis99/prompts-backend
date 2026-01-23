package org.example.sharedprompts.domain.tag;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.Prompt;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "prompt_tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_prompt_tag",
                columnNames = {"prompt_id", "tag_id"}
        ),
        indexes = {
                // 추가된 인덱스 목록 (우선순위: 필수)
                // 태그별 프롬프트 조회 최적화
                @Index(name = "idx_prompt_tags_tag", columnList = "tag_id"),
                // 프롬프트별 태그 조회 (FK이므로 자동 생성되지만 명시적 권장)
                @Index(name = "idx_prompt_tags_prompt", columnList = "prompt_id")
        })
public class PromptTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id", nullable = false)
    private Prompt prompt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public PromptTag(Prompt prompt, Tag tag) {
        this.prompt = prompt;
        this.tag = tag;
    }
}