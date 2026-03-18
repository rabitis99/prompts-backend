package org.example.sharedprompts.domain.prompt.infrastructure.policy.binding;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.CompatibilityPolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.DefaultCompatibilityPolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.CompatibilityPolicyDocument;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds validated {@link CompatibilityPolicyDocument} to runtime {@link CompatibilityPolicySource}.
 */
public final class CompatibilityPolicyBinder {

    public CompatibilityPolicySource bind(CompatibilityPolicyDocument document) {
        if (document == null) {
            return new DefaultCompatibilityPolicySource(Map.of());
        }
        Map<String, List<String>> rules = new LinkedHashMap<>(document.rules());
        return new DefaultCompatibilityPolicySource(rules);
    }
}
