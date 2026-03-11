package org.example.sharedprompts.domain.prompt.application.semantic.resolution;

import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.application.policy.AxisSourcePolicy;
import org.example.sharedprompts.domain.prompt.application.semantic.validation.SemanticValidationService;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.RequestMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 추천 플로우: Command → RecommendPromptResult */
@Component
public class RecommendationSemanticResolver {

    private final CoreSemanticResolver coreSemanticResolver;
    private final SemanticValidationService validationService;
    private final AxisSourcePolicy axisSourcePolicy;

    public RecommendationSemanticResolver(
            CoreSemanticResolver coreSemanticResolver,
            SemanticValidationService validationService,
            AxisSourcePolicy axisSourcePolicy
    ) {
        this.coreSemanticResolver = coreSemanticResolver;
        this.validationService = validationService;
        this.axisSourcePolicy = axisSourcePolicy;
    }

    public RecommendationResolutionResult.Result resolveForRecommendation(RecommendPromptCommand command) {
        if (command.requestMode() == RequestMode.EXTRACTION) {
            return RecommendationResolutionResult.Result.ok(resolveForRecommendationExtraction(command));
        }

        CoreSemanticResolver.CoreResolutionResult core = coreSemanticResolver.performCoreResolution(
                command.requestMode(),
                command.category(),
                command.intent(),
                command.roleType(),
                command.actionType(),
                (profile, intent) -> validationService.validate(command, profile, intent)
        );

        if (!core.success()) {
            return RecommendationResolutionResult.Result.fail(core.errors());
        }

        var intent = core.resolvedIntent();
        var profile = core.profile();

        Map<String, String> axisSources = axisSourcePolicy.fromResolutionMetadata(
                false,
                command.intent() != null,
                command.roleType() != null,
                command.actionType() != null,
                core.fallbackIntentUsed()
        );

        List<String> fallbackApplied = new ArrayList<>();
        if (core.fallbackIntentUsed()) {
            fallbackApplied.add("intent: profile fallback applied");
        }

        List<ActionIntent> intentCandidates = profile != null
                ? List.copyOf(profile.getAllowedIntents())
                : List.of();

        RecommendPromptResult result = new RecommendPromptResult(
                command.requestMode(),
                command.category(),
                intent,
                intentCandidates,
                core.recommendation().recommendedRole().orElse(null),
                core.recommendation().roleCandidates(),
                core.recommendation().recommendedAction().orElse(null),
                core.recommendation().actionCandidates(),
                command.tone(),
                command.style(),
                axisSources,
                core.recommendation().recommendationHints(),
                core.warnings(),
                fallbackApplied,
                intent.name()
        );
        return RecommendationResolutionResult.Result.ok(result);
    }

    private RecommendPromptResult resolveForRecommendationExtraction(RecommendPromptCommand command) {
        return new RecommendPromptResult(
                RequestMode.EXTRACTION,
                PromptCategory.EXTRACTION,
                ActionIntent.EXTRACT,
                List.of(ActionIntent.EXTRACT),
                null,
                List.of(),
                null,
                List.of(),
                command.tone(),
                command.style(),
                axisSourcePolicy.forExtraction(),
                List.of(),
                List.of(),
                List.of(),
                ActionIntent.EXTRACT.name()
        );
    }
}
