package org.example.sharedprompts.domain.prompt.domain.service;

import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ObjectiveMappingRegistry {

    /**
     * ActionType 이름 휴리스틱으로 Objective를 추론한다.
     * 명시 오버라이드가 필요한 경우 {@link ActionTypeInterface#getDefaultObjective()}를 구현하면 된다.
     * ({@code PromptSpecFactory.resolveObjective()}가 그것을 1순위로 확인)
     */
    public Optional<PromptObjective> findByActionType(ActionTypeInterface actionType) {
        if (actionType == null) {
            return Optional.empty();
        }

        PromptObjective byName = inferByActionName(actionType);
        return Optional.ofNullable(byName);
    }

    public PromptObjective getDomainDefault(TaskDomain taskDomain) {
        if (taskDomain == null) {
            return PromptObjective.REASONING;
        }
        return switch (taskDomain) {
            case TECHNICAL -> PromptObjective.REASONING;
            case ANALYTICAL -> PromptObjective.FACTUAL;
            case CREATIVE -> PromptObjective.CREATIVE_WITH_CONSTRAINTS;
            case PRACTICAL -> PromptObjective.PLANNING;
            case EDUCATIONAL -> PromptObjective.REASONING;
            case GENERAL -> PromptObjective.REASONING;
        };
    }

    private PromptObjective inferByActionName(ActionTypeInterface actionType) {
        String raw = safeName(actionType);
        if (raw.isEmpty()) {
            return null;
        }

        String name = raw.toUpperCase();

        if (hasAny(name,
                "EXTRACT", "EXTRACTION", "PARSE", "JSON", "SCHEMA",
                "STRUCTURE", "NORMALIZE", "TAG", "LABEL", "CLASSIFY",
                "KEY_VALUE", "FIELDS", "MAPPING", "REGEX", "PATTERN_EXTRACT")) {
            return PromptObjective.EXTRACTION;
        }

        if (hasAny(name,
                "SUMMARIZE", "SUMMARY", "TLDR", "ABSTRACT",
                "TRANSLATE", "TRANSLATION",
                "REWRITE", "PARAPHRASE", "POLISH", "PROOFREAD",
                "GRAMMAR", "SPELL", "CLEANUP",
                "FORMAT", "CONVERT", "TRANSFORM",
                "MINUTES", "MEETING_NOTES",
                "DATA_SUMMARY", "REPORT", "DOCUMENTATION")) {
            return PromptObjective.FACTUAL;
        }

        if (hasAny(name,
                "COMPARE", "COMPARISON", "EVALUATE", "EVALUATION",
                "REVIEW", "CRITIQUE", "PROS_CONS",
                "RISK", "TRADEOFF", "BENCHMARK",
                "ROOT_CAUSE", "DIAGNOSE", "ANALYZE", "ANALYSIS")) {
            return PromptObjective.ANALYTICAL;
        }

        if (hasAny(name,
                "PLAN", "PLANNING", "ROADMAP", "STRATEGY",
                "OUTLINE", "STRUCTURE_PLAN", "CHECKLIST",
                "SPEC", "REQUIREMENTS", "DESIGN", "ARCHITECTURE",
                "MIGRATION", "REFACTOR_PLAN",
                "TASK_BREAKDOWN", "STEPS", "WORKFLOW")) {
            return PromptObjective.PLANNING;
        }

        if (hasAny(name,
                "DRAFT", "WRITE", "GENERATE", "CREATE", "COMPOSE",
                "BRAINSTORM", "IDEATE",
                "STORY", "POEM", "SCRIPT",
                "MARKETING_COPY", "COPY", "SLOGAN",
                "TITLE", "HEADLINE",
                "CHARACTER", "SCENE")) {
            return PromptObjective.CREATIVE_WITH_CONSTRAINTS;
        }

        if (hasAny(name,
                "EXPLAIN", "TEACH", "TUTOR", "WHY", "HOW",
                "DEBUG", "TROUBLESHOOT", "FIX",
                "SOLVE", "PROBLEM_SOLVING", "DERIVE",
                "CODE_REVIEW", "REFACTOR", "OPTIMIZE",
                "ALGORITHM", "IMPLEMENT", "INTEGRATE")) {
            return PromptObjective.REASONING;
        }

        return null;
    }

    private String safeName(ActionTypeInterface actionType) {
        String s = String.valueOf(actionType);
        return s == null ? "" : s.trim();
    }

    private boolean hasAny(String name, String... tokens) {
        for (String token : tokens) {
            if (name.contains(token)) {
                return true;
            }
        }
        return false;
    }
}

