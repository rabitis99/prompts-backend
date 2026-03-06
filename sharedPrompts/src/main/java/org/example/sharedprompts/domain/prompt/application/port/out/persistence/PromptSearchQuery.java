package org.example.sharedprompts.domain.prompt.application.port.out.persistence;

import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.SortType;

/**
 * 프롬프트 검색을 위한 도메인 전용 쿼리 오브젝트.
 */
public record PromptSearchQuery(
        int page,
        int size,
        SortType sort,
        PromptCategory category,
        Long ownerId,
        Long viewerId
) {
}

