package org.example.sharedprompts.domain.prompt.application.mapping;

import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptDetailView;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;

/** Prompt + 태그·좋아요 수 → PromptDetailView 변환 */
@Component
public class PromptDetailViewMapper {

    public PromptDetailView toDetailView(Prompt prompt, List<String> tagNames, Long likeCount) {
        return new PromptDetailView(
                prompt.getId(),
                prompt.getTitle(),
                prompt.getDescription(),
                prompt.getContent(),
                prompt.getPromptCategory(),
                tagNames != null ? List.copyOf(tagNames) : List.of(),
                prompt.getAuthor().getId(),
                prompt.getAuthor().getNickname(),
                likeCount != null ? likeCount : 0L,
                prompt.getViewCount(),
                prompt.isPublic(),
                prompt.getCreatedAt().toInstant(ZoneOffset.UTC),
                prompt.getUpdatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}
