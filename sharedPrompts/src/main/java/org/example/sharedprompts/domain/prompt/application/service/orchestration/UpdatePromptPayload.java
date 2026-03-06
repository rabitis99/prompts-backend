package org.example.sharedprompts.domain.prompt.application.service.orchestration;

import java.util.List;

/**
 * 프롬프트 수정 시 애플리케이션 계층에서 사용하는 페이로드 모델.
 *
 * <p>웹 DTO(PromptUpdateDto)와 분리되어 있으며,
 * Validator 및 Service 간 데이터 전달에만 사용된다.</p>
 * <p>UpdatePromptCommand와 필드 정합: title, description, isPublic, tags, content.</p>
 */
public record UpdatePromptPayload(
        String title,
        String description,
        Boolean isPublic,
        List<String> tags,
        String content
) {
}

