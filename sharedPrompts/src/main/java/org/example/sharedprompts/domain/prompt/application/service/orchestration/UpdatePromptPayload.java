package org.example.sharedprompts.domain.prompt.application.service.orchestration;

import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;

import java.util.List;

/**
 * 프롬프트 수정 시 애플리케이션 계층에서 사용하는 페이로드 모델.
 *
 * <p>웹 DTO(PromptUpdateDto)와 분리되어 있으며,
 * Validator 및 Service 간 데이터 전달에만 사용된다.</p>
 */
public record UpdatePromptPayload(
        String title,
        String description,
        Boolean isPublic,
        PromptCategory promptCategory,
        List<String> tags
) {
}

