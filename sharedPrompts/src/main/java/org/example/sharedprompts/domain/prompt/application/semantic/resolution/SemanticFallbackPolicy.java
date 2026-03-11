package org.example.sharedprompts.domain.prompt.application.semantic.resolution;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileRegistry;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Intent 해석 정책. null이면 프로필 fallback 사용 */
@Component
public class SemanticFallbackPolicy {

    public Optional<FallbackIntentResult> resolveIntent(
            PromptCategory category,
            ActionIntent initialIntent,
            CategorySemanticProfileRegistry profileRegistry
    ) {
        if (initialIntent != null) {
            return Optional.of(new FallbackIntentResult(initialIntent, false));
        }
        Optional<CategorySemanticProfile> profileOpt = profileRegistry.getProfile(category);
        if (profileOpt.map(p -> p.getFallbackIntent() != null).orElse(false)) {
            return Optional.of(new FallbackIntentResult(profileOpt.get().getFallbackIntent(), true));
        }
        return Optional.empty();
    }

    public record FallbackIntentResult(ActionIntent intent, boolean fallbackIntentUsed) {}
}
