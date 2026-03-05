package org.example.sharedprompts.domain.prompt.adapter.in.web.mapper;

import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.BadgeDto;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.UnifiedGeneratePromptResponse;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;
import org.example.sharedprompts.domain.prompt.domain.value.quality.QualityBadge;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnifiedPromptResponseMapper {

    public UnifiedGeneratePromptResponse toResponse(UnifiedGeneratePromptResult result) {
        List<BadgeDto> badgeDtos = result.qualityBadges().stream()
                .map(this::toBadgeDto)
                .toList();

        return new UnifiedGeneratePromptResponse(
                result.output(),
                result.requestedEngineMode(),
                result.effectiveEngineMode(),
                result.engineProfile(),
                result.resolvedDomain(),
                result.objective(),
                result.outputNeeds(),
                result.intent(),
                result.variant(),
                result.coreRole(),
                result.domainRole(),
                badgeDtos,
                result.verifyPassed(),
                result.repairCount(),
                result.finallyPassed(),
                result.schemaContractFailed(),
                result.schemaFailureReasons(),
                result.appliedRuleIds(),
                result.routingReasons()
        );
    }

    private BadgeDto toBadgeDto(QualityBadge badge) {
        return new BadgeDto(badge.name(), badge.getDisplayName());
    }
}

