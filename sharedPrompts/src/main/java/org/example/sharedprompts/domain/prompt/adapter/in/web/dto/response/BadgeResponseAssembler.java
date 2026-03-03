package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response;

import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.domain.value.quality.QualityBadge;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * GeneratePromptResult → GeneratePromptResponse 변환기 (adapter 전용).
 *
 * <p><b>핵심 규칙:</b>
 * 이 클래스는 {@code adapter/in/web} 레이어에만 존재한다.
 * domain/application 클래스에서 이 변환 로직을 사용해서는 안 된다.
 *
 * <p>pass rate, repair count 등 내부 지표는 변환 시 완전히 제거된다.
 */
@Component
public class BadgeResponseAssembler {

    public GeneratePromptResponse assemble(GeneratePromptResult result) {
        List<BadgeDto> badgeDtos = result.badges().stream()
                .map(this::toBadgeDto)
                .toList();

        return new GeneratePromptResponse(
                result.promptId(),
                result.title(),
                result.generatedContent(),
                badgeDtos
        );
    }

    private BadgeDto toBadgeDto(QualityBadge badge) {
        return new BadgeDto(badge.name(), badge.getDisplayName());
    }
}
