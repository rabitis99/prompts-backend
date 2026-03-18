package org.example.sharedprompts.domain.prompt.adapter.in.web.mapper;

import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.BadgeDto;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.UnifiedGeneratePromptResponse;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.QualityBadgeItem;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.UnifiedGeneratePromptResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 프롬프트 생성 결과 → 응답 DTO 변환 매퍼
 */
@Component
public class UnifiedPromptResponseMapper {

    /**
     * 생성 결과를 API 응답 DTO로 변환
     */
    public UnifiedGeneratePromptResponse toResponse(UnifiedGeneratePromptResult result) {

        List<BadgeDto> badgeDtos = result.qualityBadges().stream()
                .map(this::toBadgeDto)
                .toList();

        return new UnifiedGeneratePromptResponse(
                result.output(),
                result.requestedEngineMode(),
                result.effectiveEngineMode(),
                result.engineProfile(),
                result.resolvedCategory(),
                result.resolvedDomain(),
                result.objective(),
                result.outputNeeds(),
                result.resolvedIntent(),
                result.variant(),
                result.resolvedRole(),
                result.resolvedAction(),
                badgeDtos,
                result.verifyPassed(),
                result.repairCount(),
                result.finallyPassed(),
                result.schemaContractFailed(),
                result.schemaFailureReasons(),
                result.semanticProfilesApplied(),
                result.validationWarnings(),
                result.recommendationHints(),
                result.semanticResolutionSummary(),
                result.axisSources()
        );
    }

    /**
     * 품질 배지(port view) → 배지 응답 DTO 변환.
     * API layer does not depend on domain.value.quality.
     */
    private BadgeDto toBadgeDto(QualityBadgeItem item) {
        return new BadgeDto(item.key(), item.displayName());
    }
}