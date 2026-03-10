package org.example.sharedprompts.domain.prompt.application.port.in.query;

import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.SortType;

/** 프롬프트 검색 쿼리 */
public record SearchPromptsQuery(
        int page,
        int size,
        SortType sort,
        PromptCategory category,
        Long ownerId,
        Long viewerId,
        String keyword
) {
}

