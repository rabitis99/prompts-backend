package org.example.sharedprompts.domain.prompt.domain.resolution;

import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ActionType 이름 휴리스틱 + TaskDomain 기본값.
 *
 * <p>해석 체인에서 명시 매핑 이후 fallback으로만 사용. Spring 의존 없음.
 * {@link ObjectiveMappingRegistryPort} 구현체.
 */
public class ObjectiveMappingRegistry implements ObjectiveMappingRegistryPort {

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

    @Override
    public Optional<PromptObjective> findByActionType(ActionTypeInterface actionType) {
        if (actionType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(inferByActionName(actionType));
    }

    @Override
    public PromptObjective getDomainDefault(TaskDomain taskDomain) {
        if (taskDomain == null) {
            return PromptObjective.REASONING;
        }
        return switch (taskDomain) {
            case TECHNICAL   -> PromptObjective.REASONING;
            case ANALYTICAL  -> PromptObjective.FACTUAL;
            case CREATIVE    -> PromptObjective.CREATIVE_WITH_CONSTRAINTS;
            case PRACTICAL   -> PromptObjective.PLANNING;
            case EDUCATIONAL -> PromptObjective.REASONING;
            case GENERAL     -> PromptObjective.REASONING;
        };
    }

    private PromptObjective inferByActionName(ActionTypeInterface actionType) {
        String name = String.valueOf(actionType).trim().toUpperCase();
        if (name.isEmpty()) {
            return null;
        }
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
