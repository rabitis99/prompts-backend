package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.ObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter: {@link ObjectiveMappingRegistryPort} implemented using {@link ObjectivePolicySource}.
 * Explicit mapping and domain default come from the policy source; heuristic remains here for fallback.
 * Config must not hold raw maps; wire {@link ObjectivePolicySource} (e.g. {@link org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.DefaultObjectivePolicySource}).
 */
public class ObjectiveMappingRegistry implements ObjectiveMappingRegistryPort {

    private final ObjectivePolicySource policySource;
    /** Optional overlay (e.g. tests); checked before policy source. */
    private final Map<String, PromptObjective> overlayByStableKey = new ConcurrentHashMap<>();

    private static final Map<PromptObjective, Set<String>> KEYWORD_MAP;

    static {
        KEYWORD_MAP = new LinkedHashMap<>();
        KEYWORD_MAP.put(PromptObjective.EXTRACTION, Set.of(
                "EXTRACT", "EXTRACTION", "PARSE", "JSON", "SCHEMA",
                "STRUCTURE", "NORMALIZE", "TAG", "LABEL", "CLASSIFY",
                "KEY_VALUE", "FIELDS", "MAPPING", "REGEX", "PATTERN_EXTRACT"
        ));
        KEYWORD_MAP.put(PromptObjective.FACTUAL, Set.of(
                "SUMMARIZE", "SUMMARY", "TLDR", "ABSTRACT",
                "TRANSLATE", "TRANSLATION",
                "REWRITE", "PARAPHRASE", "POLISH", "PROOFREAD",
                "GRAMMAR", "SPELL", "CLEANUP",
                "FORMAT", "CONVERT", "TRANSFORM",
                "MINUTES", "MEETING_NOTES",
                "DATA_SUMMARY", "REPORT", "DOCUMENTATION"
        ));
        KEYWORD_MAP.put(PromptObjective.ANALYTICAL, Set.of(
                "COMPARE", "COMPARISON", "EVALUATE", "EVALUATION",
                "REVIEW", "CRITIQUE", "PROS_CONS",
                "RISK", "TRADEOFF", "BENCHMARK",
                "ROOT_CAUSE", "DIAGNOSE", "ANALYZE", "ANALYSIS"
        ));
        KEYWORD_MAP.put(PromptObjective.PLANNING, Set.of(
                "PLAN", "PLANNING", "ROADMAP", "STRATEGY",
                "OUTLINE", "STRUCTURE_PLAN", "CHECKLIST",
                "SPEC", "REQUIREMENTS", "DESIGN", "ARCHITECTURE",
                "MIGRATION", "REFACTOR_PLAN",
                "TASK_BREAKDOWN", "STEPS", "WORKFLOW"
        ));
        KEYWORD_MAP.put(PromptObjective.CREATIVE_WITH_CONSTRAINTS, Set.of(
                "DRAFT", "WRITE", "GENERATE", "CREATE", "COMPOSE",
                "BRAINSTORM", "IDEATE",
                "STORY", "POEM", "SCRIPT",
                "MARKETING_COPY", "COPY", "SLOGAN",
                "TITLE", "HEADLINE",
                "CHARACTER", "SCENE"
        ));
        KEYWORD_MAP.put(PromptObjective.REASONING, Set.of(
                "EXPLAIN", "TEACH", "TUTOR", "WHY", "HOW",
                "DEBUG", "TROUBLESHOOT", "FIX",
                "SOLVE", "PROBLEM_SOLVING", "DERIVE",
                "CODE_REVIEW", "REFACTOR", "OPTIMIZE",
                "ALGORITHM", "IMPLEMENT", "INTEGRATE"
        ));
    }

    public ObjectiveMappingRegistry(CanonicalActionRegistry canonicalActionRegistry, ObjectivePolicySource policySource) {
        this.policySource = policySource;
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
        return Optional.ofNullable(inferByActionName(actionType));
    }

    @Override
    public PromptObjective getDomainDefault(TaskDomain taskDomain) {
        return policySource.getDomainDefault(taskDomain);
    }

    private PromptObjective inferByActionName(ActionTypeInterface actionType) {
        String name = String.valueOf(actionType).trim().toUpperCase();
        if (name.isEmpty()) {
            return null;
        }
        // 1) 정확 매칭 우선 (예: CODE_REVIEW)
        for (Map.Entry<PromptObjective, Set<String>> entry : KEYWORD_MAP.entrySet()) {
            if (entry.getValue().contains(name)) {
                return entry.getKey();
            }
        }
        // 2) 부분 매칭 fallback
        for (Map.Entry<PromptObjective, Set<String>> entry : KEYWORD_MAP.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (name.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}
