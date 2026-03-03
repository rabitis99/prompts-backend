package org.example.sharedprompts.domain.prompt.application.service.orchestration;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.request.PromptUpdateDto;
import org.example.sharedprompts.global.util.HtmlSanitizer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PromptSanitizationService {

    private final HtmlSanitizer htmlSanitizer;

    /**
     * HTML 관련 필드(title, description, input)에 대해서만 Sanitization을 수행하면서
     * 원본 DTO는 변경하지 않고 복사본을 만들어 반환한다.
     */
    public PromptRequestDto sanitize(PromptRequestDto request) {
        Objects.requireNonNull(request, "request must not be null");

        return request.toBuilder()
                .title(sanitizeTitle(request.getTitle()))
                .description(sanitizeDescription(request.getDescription()))
                .input(sanitizeInput(request.getInput()))
                .tags(request.getTags() != null ? List.copyOf(request.getTags()) : null)
                .build();
    }

    /**
     * 업데이트 DTO도 마찬가지로 복사본을 생성해서 반환한다.
     * HTML 관련 필드(title, description)만 정제 대상이다.
     */
    public PromptUpdateDto sanitize(PromptUpdateDto dto) {
        Objects.requireNonNull(dto, "dto must not be null");

        String sanitizedTitle = sanitizeTitle(dto.getTitle());
        String sanitizedDescription = sanitizeDescription(dto.getDescription());

        return dto.toBuilder()
                .title(sanitizedTitle)
                .description(sanitizedDescription)
                .tags(dto.getTags() != null ? List.copyOf(dto.getTags()) : null)
                .build();
    }

    // 개별 필드 Sanitization 메서드들 - 재사용 및 테스트 용이성을 위해 분리
    public String sanitizeTitle(String title) {
        return title != null ? htmlSanitizer.sanitize(title) : null;
    }

    public String sanitizeDescription(String description) {
        return description != null ? htmlSanitizer.sanitize(description) : null;
    }

    public String sanitizeInput(String input) {
        return input != null ? htmlSanitizer.sanitize(input) : null;
    }
}
