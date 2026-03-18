package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.application.semantic.experiment.ExperimentContext;
import org.example.sharedprompts.domain.prompt.application.semantic.experiment.ExperimentPolicySelector;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Selects policy version by experiment: variant from selector maps to a PolicyVersion.
 * Deterministic: same context → same variant → same version.
 */
public final class ExperimentPolicySelectionStrategy implements PolicySelectionStrategy {

    private final PolicyVersion defaultVersion;
    private final Map<String, PolicyVersion> variantToVersion;
    private final ExperimentPolicySelector selector;

    public ExperimentPolicySelectionStrategy(
            PolicyVersion defaultVersion,
            Map<String, PolicyVersion> variantToVersion,
            ExperimentPolicySelector selector
    ) {
        this.defaultVersion = Objects.requireNonNull(defaultVersion, "defaultVersion");
        this.variantToVersion = variantToVersion != null ? Map.copyOf(variantToVersion) : Map.of();
        this.selector = selector != null ? selector : ctx -> Optional.empty();
    }

    @Override
    public PolicyVersion selectPolicyVersion(ExperimentContext requestContext) {
        Optional<String> variant = selector.determineVariant(
                requestContext != null ? requestContext : ExperimentContext.empty()
        );
        if (variant.isEmpty() || variant.get().isBlank()) {
            return defaultVersion;
        }
        return variantToVersion.getOrDefault(variant.get().trim(), defaultVersion);
    }
}
