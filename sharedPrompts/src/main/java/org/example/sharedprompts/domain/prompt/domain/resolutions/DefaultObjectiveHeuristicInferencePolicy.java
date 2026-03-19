package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default heuristic inference:
 * exact keyword match first, then partial containment fallback.
 */
public class DefaultObjectiveHeuristicInferencePolicy implements ObjectiveHeuristicInferencePolicy {

    private static final Map<PromptObjective, Set<String>> KEYWORD_MAP;

    static {
        Map<PromptObjective, Set<String>> m = new LinkedHashMap<>();
        m.put(PromptObjective.EXTRACTION, Set.of(
                "EXTRACT", "EXTRACTION", "PARSE", "JSON", "SCHEMA",
                "STRUCTURE", "NORMALIZE", "TAG", "LABEL", "CLASSIFY",
                "KEY_VALUE", "FIELDS", "MAPPING", "REGEX", "PATTERN_EXTRACT"
        ));
        m.put(PromptObjective.FACTUAL, Set.of(
                "SUMMARIZE", "SUMMARY", "TLDR", "ABSTRACT",
                "TRANSLATE", "TRANSLATION",
                "REWRITE", "PARAPHRASE", "POLISH", "PROOFREAD",
                "GRAMMAR", "SPELL", "CLEANUP",
                "FORMAT", "CONVERT", "TRANSFORM",
                "MINUTES", "MEETING_NOTES",
                "DATA_SUMMARY", "REPORT", "DOCUMENTATION"
        ));
        m.put(PromptObjective.ANALYTICAL, Set.of(
                "COMPARE", "COMPARISON", "EVALUATE", "EVALUATION",
                "REVIEW", "CRITIQUE", "PROS_CONS",
                "RISK", "TRADEOFF", "BENCHMARK",
                "ROOT_CAUSE", "DIAGNOSE", "ANALYZE", "ANALYSIS"
        ));
        m.put(PromptObjective.PLANNING, Set.of(
                "PLAN", "PLANNING", "ROADMAP", "STRATEGY",
                "OUTLINE", "STRUCTURE_PLAN", "CHECKLIST",
                "SPEC", "REQUIREMENTS", "DESIGN", "ARCHITECTURE",
                "MIGRATION", "REFACTOR_PLAN",
                "TASK_BREAKDOWN", "STEPS", "WORKFLOW"
        ));
        m.put(PromptObjective.CREATIVE_WITH_CONSTRAINTS, Set.of(
                "DRAFT", "WRITE", "GENERATE", "CREATE", "COMPOSE",
                "BRAINSTORM", "IDEATE",
                "STORY", "POEM", "SCRIPT",
                "MARKETING_COPY", "COPY", "SLOGAN",
                "TITLE", "HEADLINE",
                "CHARACTER", "SCENE"
        ));
        m.put(PromptObjective.REASONING, Set.of(
                "EXPLAIN", "TEACH", "TUTOR", "WHY", "HOW",
                "DEBUG", "TROUBLESHOOT", "FIX",
                "SOLVE", "PROBLEM_SOLVING", "DERIVE",
                "CODE_REVIEW", "REFACTOR", "OPTIMIZE",
                "ALGORITHM", "IMPLEMENT", "INTEGRATE"
        ));
        KEYWORD_MAP = Map.copyOf(m);
    }

    @Override
    public Optional<PromptObjective> inferByActionName(ActionTypeInterface actionType) {
        if (actionType == null) return Optional.empty();
        String name = String.valueOf(actionType).trim().toUpperCase();
        if (name.isEmpty()) return Optional.empty();

        // 1) exact match first (e.g. CODE_REVIEW)
        for (Map.Entry<PromptObjective, Set<String>> entry : KEYWORD_MAP.entrySet()) {
            if (entry.getValue().contains(name)) {
                return Optional.of(entry.getKey());
            }
        }
        // 2) partial match fallback
        for (Map.Entry<PromptObjective, Set<String>> entry : KEYWORD_MAP.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (name.contains(keyword)) {
                    return Optional.of(entry.getKey());
                }
            }
        }
        return Optional.empty();
    }
}

