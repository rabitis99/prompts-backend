package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.ObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

/**
 * Adapter: {@link ObjectiveMappingRegistryPort} implemented using {@link ObjectivePolicySource}.
 * Explicit mapping and domain default come from the policy source; heuristic fallback is delegated to
 * {@link ObjectiveHeuristicInferencePolicy}.
 * Config must not hold raw maps; wire {@link ObjectivePolicySource} (e.g. {@link org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.DefaultObjectivePolicySource})
 * and {@link ObjectiveHeuristicInferencePolicy} (e.g. {@link DefaultObjectiveHeuristicInferencePolicy}).
 */
public class ObjectiveMappingRegistry implements ObjectiveMappingRegistryPort {

    private final ObjectivePolicySource policySource;
    private final ObjectiveHeuristicInferencePolicy heuristicInferencePolicy;
    /** Optional overlay (e.g. tests); checked before policy source. */
    private final Map<String, PromptObjective> overlayByStableKey = new ConcurrentHashMap<>();

    public ObjectiveMappingRegistry(
            ObjectivePolicySource policySource,
            ObjectiveHeuristicInferencePolicy heuristicInferencePolicy) {
        this.policySource = Objects.requireNonNull(policySource, "policySource");
        this.heuristicInferencePolicy = Objects.requireNonNull(heuristicInferencePolicy, "heuristicInferencePolicy");
    }

    /** Test/config overlay: add explicit mapping by stable key without changing policy source. */
    public void putByStableKey(String stableKey, PromptObjective objective) {
        if (stableKey == null || stableKey.isBlank() || objective == null) return;
        String key = stableKey.trim();
        PromptObjective previous = overlayByStableKey.putIfAbsent(key, objective);
        if (previous != null && !previous.equals(objective)) {
            throw new IllegalStateException(
                    "Conflicting overlay objective for " + stableKey + ": " + previous + " vs " + objective);
        }
    }

    /** Test/config overlay: add by action type (uses action stable key). */
    public void put(ActionTypeInterface actionType, PromptObjective objective) {
        if (actionType != null && objective != null) {
            putByStableKey(actionType.key(), objective);
        }
    }

    @Override
    public Optional<PromptObjective> findByActionType(ActionTypeInterface actionType) {
        if (actionType == null) return Optional.empty();
        String key = actionType.key();
        Optional<PromptObjective> fromOverlay = Optional.ofNullable(overlayByStableKey.get(key));
        if (fromOverlay.isPresent()) return fromOverlay;
        Optional<PromptObjective> fromSource = policySource.findByStableKey(key);
        if (fromSource.isPresent()) return fromSource;
        return heuristicInferencePolicy.inferByActionName(actionType);
    }

    @Override
    public PromptObjective getDomainDefault(TaskDomain taskDomain) {
        return policySource.getDomainDefault(taskDomain);
    }
}
