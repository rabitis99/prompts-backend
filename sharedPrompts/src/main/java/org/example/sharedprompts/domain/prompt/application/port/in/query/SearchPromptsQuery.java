package org.example.sharedprompts.domain.prompt.application.port.in.query;

import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.SortType;

/**
 * 애플리케이션 계층에서 사용하는 프롬프트 검색 쿼리.
 *
 * <p>웹 DTO(PromptSearchCondition)와 분리되어 있으며,
 * 필요한 경우 어댑터에서 이 타입으로 변환한다.</p>
 */
public record SearchPromptsQuery(
        int page,
        int size,
        SortType sort,
        PromptCategory category,
        Long ownerId,
        Long viewerId
) {
}

