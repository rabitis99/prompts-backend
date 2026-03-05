package org.example.sharedprompts.domain.prompt.application.port.in.query;

import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.quality.QualityBadge;

import java.util.List;

/**
 * 프롬프트 생성 유즈케이스 결과.
 *
 * <p>수치 지표(passRate, repairCount 등)는 내부 측정용으로만 유지되며,
 * UX 응답 변환 시 {@code BadgeResponseAssembler}가 배지만 추출한다.
 *
 * <p>{@code formatValid}는 Verify 단계의 FORMAT_COMPLIANCE 루브릭 결과를 반영하며,
 * SchemaContractEvaluator 등에서 스키마/출력 계약 준수 여부 판단에 직접 사용한다.</p>
 */
public record GeneratePromptResult(
        Long promptId,
        String title,
        String generatedContent,
        List<QualityBadge> badges,
        PromptObjective objective,

        /** Verify 단계 FORMAT_COMPLIANCE 루브릭 통과 여부. 스키마/출력 계약 평가에 사용. */
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
