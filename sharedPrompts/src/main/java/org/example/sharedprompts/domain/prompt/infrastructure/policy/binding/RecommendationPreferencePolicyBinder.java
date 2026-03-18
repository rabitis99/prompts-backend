package org.example.sharedprompts.domain.prompt.infrastructure.policy.binding;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ActionRecommendationPreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultActionRecommendationPreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.RecommendationPreferencePolicyDocument;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds validated {@link RecommendationPreferencePolicyDocument} to runtime {@link ActionRecommendationPreferenceSource}.
 */
public final class RecommendationPreferencePolicyBinder {

    public ActionRecommendationPreferenceSource bind(RecommendationPreferencePolicyDocument document) {
        if (document == null) {
            return new DefaultActionRecommendationPreferenceSource(Map.of());
        }
        Map<String, List<String>> rules = new LinkedHashMap<>(document.rules());
        return new DefaultActionRecommendationPreferenceSource(rules);
    }
}
