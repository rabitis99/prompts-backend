package org.example.sharedprompts.domain.prompt.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 프롬프트 생성 응답 DTO (adapter/in/web 전용).
 *
 * <p><b>노출 규칙:</b>
 * <ul>
 *   <li>pass rate, repair rate, repair count 등 수치 지표는 절대 포함하지 않는다.</li>
 *   <li>품질 정보는 {@link BadgeDto} 형태로만 노출한다.</li>
 * </ul>
 */
public record GeneratePromptResponse(
        Long id,
        String title,
        String content,
        @JsonProperty("quality_badges")
        List<BadgeDto> qualityBadges
) {

    /** 배지 DTO — UX에 표시될 배지 정보만 담는다. */
    public record BadgeDto(
            String code,
            @JsonProperty("display_name")
            String displayName
    ) {}
}
