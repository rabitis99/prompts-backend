package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionId;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 명시 매핑 + 액션 이름 휴리스틱 + TaskDomain 기본값.
 * 명시 매핑은 canonical action 기준으로 저장; 조회 시 action → canonical → 명시 → 휴리스틱 → 도메인 기본 순.
 *
 * <p>Config에서 put()으로 명시 매핑을 등록한 뒤, 조회 시 canonical 해석 후 명시 → 휴리스틱 → 도메인 기본 순으로 사용.
 * {@link ObjectiveMappingRegistryPort} 유일 구현체.
 */
public class ObjectiveMappingRegistry implements ObjectiveMappingRegistryPort {

    private final CanonicalActionRegistry canonicalActionRegistry;
    private final Map<CanonicalActionId, PromptObjective> explicitByCanonical = new ConcurrentHashMap<>();

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

    public ObjectiveMappingRegistry(CanonicalActionRegistry canonicalActionRegistry) {
        this.canonicalActionRegistry = canonicalActionRegistry;
    }

    /** Config/테스트 전용. 명시 매핑 등록; canonical로 저장되어 동일 capability의 다른 action도 동일 objective 사용. */
    public void put(ActionTypeInterface actionType, PromptObjective objective) {
        if (actionType != null && objective != null) {
            canonicalActionRegistry.toCanonical(actionType).ifPresent(c -> explicitByCanonical.put(c, objective));
        }
    }

    @Override
    public Optional<PromptObjective> findByActionType(ActionTypeInterface actionType) {
        if (actionType == null) {
            return Optional.empty();
        }
        Optional<CanonicalActionId> canonical = canonicalActionRegistry.toCanonical(actionType);
        if (canonical.isPresent()) {
            PromptObjective explicit = explicitByCanonical.get(canonical.get());
            if (explicit != null) {
                return Optional.of(explicit);
            }
        }
        return Optional.ofNullable(inferByActionName(actionType));
    }

    @Override
    public PromptObjective getDomainDefault(TaskDomain taskDomain) {
        if (taskDomain == null) {
            return PromptObjective.REASONING;
        }
        return switch (taskDomain) {
            case TECHNICAL, EDUCATIONAL, GENERAL -> PromptObjective.REASONING;
            case ANALYTICAL  -> PromptObjective.ANALYTICAL;
            case CREATIVE    -> PromptObjective.CREATIVE_WITH_CONSTRAINTS;
            case PRACTICAL   -> PromptObjective.PLANNING;
        };
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
