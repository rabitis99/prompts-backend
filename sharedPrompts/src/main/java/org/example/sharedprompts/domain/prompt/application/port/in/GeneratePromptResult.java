package org.example.sharedprompts.domain.prompt.application.port.in;

import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;

import java.util.List;

/**
 * 프롬프트 생성 유즈케이스 결과.
 *
 * <p>수치 지표(passRate, repairCount 등)는 내부 측정용으로만 유지되며,
 * UX 응답 변환 시 {@code BadgeResponseAssembler}가 배지만 추출한다.
 */
public record GeneratePromptResult(
        Long promptId,
        String title,
        String generatedContent,
        List<QualityBadge> badges,
        PromptObjective objective,

        // ─── 내부 측정 지표 (UX에 노출하지 않음) ───
        boolean firstPassSuccess,
        int repairCount,
        boolean finallyPassed
) {
    public GeneratePromptResult {
        badges = badges != null ? List.copyOf(badges) : List.of();
    }

    public boolean wasRepaired() {
        return repairCount > 0;
    }
}
