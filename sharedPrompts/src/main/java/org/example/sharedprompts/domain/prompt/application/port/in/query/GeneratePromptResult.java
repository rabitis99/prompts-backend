package org.example.sharedprompts.domain.prompt.application.port.in.query;

import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.quality.QualityBadge;

import java.util.List;

/**
 * 프롬프트 생성 유즈케이스 결과.
 */
public record GeneratePromptResult(
        Long promptId,
        String title,
        String generatedContent,
        List<QualityBadge> badges,
        PromptObjective objective,
        boolean formatValid,

        // ─── 내부 측정 지표 (UX에 노출하지 않음) ───
        boolean firstPassSuccess,
        int repairCount,
        boolean finallyPassed
) {
    public GeneratePromptResult {
        badges = badges != null ? List.copyOf(badges) : List.of();
    }
}
