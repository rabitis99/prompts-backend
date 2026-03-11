package org.example.sharedprompts.domain.prompt.application.semantic.resolution;

import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.RequestMode;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.RecommendationResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticValidationResult;
import org.example.sharedprompts.domain.prompt.application.semantic.recommendation.SemanticRecommendationService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/** 시맨틱 해석 공통 코디네이터. 프로필·Intent·검증·추천 위임 */
@Component
public class CoreSemanticResolver {

    private final CategorySemanticProfileRegistry profileRegistry;
    private final SemanticRecommendationService recommendationService;
    private final SemanticRequestModeValidator requestModeValidator;
    private final SemanticFallbackPolicy fallbackPolicy;

    public CoreSemanticResolver(
            CategorySemanticProfileRegistry profileRegistry,
            SemanticRecommendationService recommendationService,
            SemanticRequestModeValidator requestModeValidator,
            SemanticFallbackPolicy fallbackPolicy
    ) {
        this.profileRegistry = profileRegistry;
        this.recommendationService = recommendationService;
        this.requestModeValidator = requestModeValidator;
        this.fallbackPolicy = fallbackPolicy;
    }

    public CoreResolutionResult performCoreResolution(
            RequestMode requestMode,
            PromptCategory category,
            ActionIntent initialIntent,
            org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface roleType,
            org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface actionType,
            BiFunction<CategorySemanticProfile, ActionIntent, SemanticValidationResult> validator
    ) {
        List<String> validationErrors = requestModeValidator.validateForResolution(category, requestMode);
        if (!validationErrors.isEmpty()) {
            return CoreResolutionResult.fail(validationErrors);
        }

        Optional<SemanticFallbackPolicy.FallbackIntentResult> fallbackResultOpt =
                fallbackPolicy.resolveIntent(category, initialIntent, profileRegistry);
        if (fallbackResultOpt.isEmpty()) {
            return CoreResolutionResult.fail(List.of("intent is required for SIMPLE/ADVANCED"));
        }
        ActionIntent intent = fallbackResultOpt.get().intent();
        boolean fallbackIntentUsed = fallbackResultOpt.get().fallbackIntentUsed();

        CategorySemanticProfile profile = profileRegistry.getProfile(category).orElse(null);

        SemanticValidationResult validation = validator.apply(profile, intent);
        if (validation.severity() == SemanticValidationResult.Severity.ERROR) {
            List<String> messages = validation.items().stream()
                    .map(i -> i.code() + ": " + i.message())
                    .toList();
            return CoreResolutionResult.fail(messages);
        }

        var recommendation = recommendationService.recommend(
                category,
                intent,
                profile,
                roleType,
                actionType,
                fallbackIntentUsed
        );

        List<String> warnings = new ArrayList<>();
        if (validation.severity() == SemanticValidationResult.Severity.WARNING) {
            validation.items().forEach(i -> warnings.add(i.message()));
        }

        return CoreResolutionResult.ok(intent, profile, fallbackIntentUsed, validation, recommendation, warnings);
    }

    public record CoreResolutionResult(
            boolean success,
            List<String> errors,
            ActionIntent resolvedIntent,
            CategorySemanticProfile profile,
            boolean fallbackIntentUsed,
            SemanticValidationResult validation,
            RecommendationResult recommendation,
            List<String> warnings
    ) {
        static CoreResolutionResult fail(List<String> errors) {
            return new CoreResolutionResult(false, errors, null, null, false, null, null, null);
        }

        static CoreResolutionResult ok(
                ActionIntent resolvedIntent,
                CategorySemanticProfile profile,
                boolean fallbackIntentUsed,
                SemanticValidationResult validation,
                RecommendationResult recommendation,
                List<String> warnings
        ) {
            return new CoreResolutionResult(true, null, resolvedIntent, profile, fallbackIntentUsed, validation, recommendation, warnings);
        }
    }
}
