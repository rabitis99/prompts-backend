package org.example.sharedprompts.domain.prompt.domain.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.domain.model.Constraints;
import org.example.sharedprompts.domain.prompt.domain.model.ContentSandbox;
import org.example.sharedprompts.domain.prompt.domain.model.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSection;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.policy.StrategyBundlePolicy;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.PromptStrategyBundle;
import org.example.sharedprompts.domain.prompt.domain.value.QualityPriority;
import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.guideline.GuidelineRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 입력을 받아 {@link PromptSpec}을 생성하는 도메인 팩토리.
 *
 * <p>Objective 자동 매핑 원칙 — 사용자는 Objective를 직접 선택하지 않는다.
 * TaskDomain/ActionType에 따라 내부에서 자동 결정된다.
 */
@Component
@RequiredArgsConstructor
public class PromptSpecFactory {

    private final StrategyBundlePolicy strategyBundlePolicy;
    private final ObjectiveMappingRegistry objectiveMappingRegistry;

    /**
     * 사용자 입력으로부터 PromptSpec을 생성한다.
     */
    public PromptSpec create(
            String rawInput,
            TaskDomain taskDomain,
            ActionTypeInterface actionType,
            RoleTypeInterface role,
            ToneType tone,
            StyleType style,
            LanguageType locale,
            boolean experimentalEnabled
    ) {
        return create(rawInput, taskDomain, actionType, role, tone, style, locale,
                experimentalEnabled, ExperienceLevel.INTERMEDIATE, null);
    }

    public PromptSpec create(
            String rawInput,
            TaskDomain taskDomain,
            ActionTypeInterface actionType,
            RoleTypeInterface role,
            ToneType tone,
            StyleType style,
            LanguageType locale,
            boolean experimentalEnabled,
            ExperienceLevel experienceLevel
    ) {
        return create(rawInput, taskDomain, actionType, role, tone, style, locale,
                experimentalEnabled, experienceLevel, null);
    }

    /**
     * 전체 파라미터 버전.
     *
     * @param jsonSchema EXTRACTION 시 사용할 JSON Schema (null이면 기본 스키마 사용)
     */
    public PromptSpec create(
            String rawInput,
            TaskDomain taskDomain,
            ActionTypeInterface actionType,
            RoleTypeInterface role,
            ToneType tone,
            StyleType style,
            LanguageType locale,
            boolean experimentalEnabled,
            ExperienceLevel experienceLevel,
            String jsonSchema
    ) {
        if (rawInput == null) {
            throw new IllegalArgumentException("rawInput은 null일 수 없습니다.");
        }

        ExperienceLevel level = experienceLevel != null ? experienceLevel : ExperienceLevel.INTERMEDIATE;

        PromptObjective objective = resolveObjective(taskDomain, actionType);
        QualityPriority priority = resolvePriority(objective);
        QualityRubric rubric = buildRubric(objective);
        List<PromptSection> sections = buildSections(objective, role, taskDomain, locale);
        Constraints constraints = buildConstraints(objective, level);
        OutputContract outputContract = buildOutputContract(objective, jsonSchema);
        PromptStrategyBundle bundle = strategyBundlePolicy.resolveBundle(objective, experimentalEnabled);

        return PromptSpec.builder()
                .objective(objective)
                .priority(priority)
                .rubric(rubric)
                .taskDomain(taskDomain)
                .experienceLevel(level)
                .sections(sections)
                .constraints(constraints)
                .outputContract(outputContract)
                .contentSandbox(ContentSandbox.defaults())
                .role(role)
                .tone(tone != null ? tone : ToneType.NEUTRAL)
                .style(style != null ? style : StyleType.NARRATIVE)
                .strategyBundle(bundle)
                .locale(locale != null ? locale : LanguageType.KOREAN)
                .rawInput(rawInput)
                .actionType(actionType)
                .build();
    }

    /**
     * TaskDomain과 ActionType으로부터 Objective를 결정한다.
     * 우선순위:
     * 1) ActionType 기본 Objective
     * 2) ObjectiveMappingRegistry 명시/휴리스틱 매핑
     * 3) TaskDomain 기본값
     */
    PromptObjective resolveObjective(TaskDomain taskDomain, ActionTypeInterface actionType) {
        if (actionType != null) {
            PromptObjective defaultObjective = actionType.getDefaultObjective();
            if (defaultObjective != null) {
                return defaultObjective;
            }
        }

        return objectiveMappingRegistry
                .findByActionType(actionType)
                .orElseGet(() -> objectiveMappingRegistry.getDomainDefault(taskDomain));
    }

    private QualityPriority resolvePriority(PromptObjective objective) {
        return switch (objective) {
            case FACTUAL, REASONING, ANALYTICAL -> QualityPriority.ACCURACY_FIRST;
            case EXTRACTION, PLANNING -> QualityPriority.STRUCTURE_FIRST;
            case CREATIVE_WITH_CONSTRAINTS -> QualityPriority.CREATIVITY_SECOND;
        };
    }

    QualityRubric buildRubric(PromptObjective objective) {
        List<QualityRubric.RubricItem> items = new ArrayList<>();

        // 모든 Objective 공통 필수 항목
        items.add(QualityRubric.RubricItem.COVERAGE);
        items.add(QualityRubric.RubricItem.NO_PROHIBITED_CONTENT);

        // Objective별 추가 항목
        switch (objective) {
            case FACTUAL -> {
                items.add(QualityRubric.RubricItem.INPUT_PRESERVATION);
                items.add(QualityRubric.RubricItem.NO_CONTRADICTION);
                items.add(QualityRubric.RubricItem.UNCERTAINTY_HANDLING);
            }
            case REASONING -> {
                items.add(QualityRubric.RubricItem.NO_CONTRADICTION);
                items.add(QualityRubric.RubricItem.UNCERTAINTY_HANDLING);
            }
            case EXTRACTION -> {
                items.add(QualityRubric.RubricItem.FORMAT_COMPLIANCE);
                items.add(QualityRubric.RubricItem.INPUT_PRESERVATION);
            }
            case PLANNING -> {
                items.add(QualityRubric.RubricItem.INPUT_PRESERVATION);
                items.add(QualityRubric.RubricItem.NO_CONTRADICTION);
            }
            case CREATIVE_WITH_CONSTRAINTS -> {
                items.add(QualityRubric.RubricItem.FORMAT_COMPLIANCE);
                // CREATIVE_WITH_CONSTRAINTS: 의미적 창의성 판단은 제외 (Soft-verify)
            }
            case ANALYTICAL -> {
                // ANALYTICAL: 분석 근거 보존 + 모순 없음 + 불확실성 처리 (CHAIN_OF_VERIFICATION)
                items.add(QualityRubric.RubricItem.INPUT_PRESERVATION);
                items.add(QualityRubric.RubricItem.NO_CONTRADICTION);
                items.add(QualityRubric.RubricItem.UNCERTAINTY_HANDLING);
            }
        }

        return QualityRubric.of(items);
    }

    private List<PromptSection> buildSections(
            PromptObjective objective,
            RoleTypeInterface role,
            TaskDomain taskDomain,
            LanguageType locale
    ) {
        List<PromptSection> sections = new ArrayList<>();

        if (role != null) {
            sections.add(PromptSection.required(
                    PromptSection.SectionType.ROLE,
                    "You are a " + role.getRoleNameEn() + ". " + role.getDescriptionEn()
            ));
        }

        // TaskDomain + GuidelinePolicy를 사용해 구조/품질 규칙을 섹션으로 흡수
        StringBuilder checklistBuilder = new StringBuilder();
        appendGuidelineRules(checklistBuilder, taskDomain.principles(), locale);
        appendGuidelineRules(checklistBuilder, taskDomain.structuringRules(), locale);
        appendGuidelineRules(checklistBuilder, taskDomain.outputConstraints(), locale);

        if (checklistBuilder.length() > 0) {
            sections.add(PromptSection.required(
                    PromptSection.SectionType.VERIFICATION_CHECKLIST,
                    checklistBuilder.toString()
            ));
        }

        // Objective별 지시 방향을 섹션 내용으로 제공 (Renderer가 메타프롬프트에 포함)
        sections.add(PromptSection.required(
                PromptSection.SectionType.INSTRUCTION,
                buildInstructionContent(objective)));

        if (objective == PromptObjective.EXTRACTION) {
            sections.add(PromptSection.required(PromptSection.SectionType.OUTPUT_FORMAT, ""));
        }

        if (objective == PromptObjective.PLANNING) {
            sections.add(PromptSection.optional(PromptSection.SectionType.CONSTRAINTS, ""));
        }

        return sections;
    }

    private void appendGuidelineRules(StringBuilder sb, List<GuidelineRule> rules, LanguageType locale) {
        if (rules == null || rules.isEmpty()) {
            return;
        }
        for (GuidelineRule rule : rules) {
            String description = switch (locale != null ? locale : LanguageType.KOREAN) {
                case ENGLISH -> rule.description().en();
                case JAPANESE -> rule.description().ja();
                default -> rule.description().ko();
            };
            sb.append("- ").append(description).append("\n");
        }
    }

    private Constraints buildConstraints(PromptObjective objective, ExperienceLevel level) {
        ExperienceLevel effectiveLevel = level != null ? level : ExperienceLevel.INTERMEDIATE;

        int baseMaxLength;
        boolean requireStepByStep = false;
        boolean requireCitations = false;

        switch (objective) {
            case FACTUAL -> {
                baseMaxLength = 3000;
                requireCitations = true;
            }
            case REASONING -> {
                baseMaxLength = 2500;
                requireStepByStep = true;
            }
            case EXTRACTION -> {
                baseMaxLength = 1000;
            }
            case PLANNING -> {
                baseMaxLength = 3000;
                requireStepByStep = true;
            }
            case CREATIVE_WITH_CONSTRAINTS -> {
                baseMaxLength = 2000;
            }
            case ANALYTICAL -> {
                baseMaxLength = 3000;
                requireCitations = true;  // 분석은 근거 필요
            }
        }

        double factor = switch (effectiveLevel) {
            case BEGINNER -> 1.2;
            case INTERMEDIATE -> 1.0;
            case ADVANCED -> 0.9;
            case EXPERT -> 0.75;
        };

        int adjustedMaxLength = (int) Math.round(baseMaxLength * factor);

        if (effectiveLevel == ExperienceLevel.BEGINNER) {
            requireStepByStep = true;
        }
        if (effectiveLevel == ExperienceLevel.EXPERT &&
                (objective == PromptObjective.FACTUAL || objective == PromptObjective.REASONING)) {
            requireCitations = true;
        }

        return Constraints.builder()
                .maxLength(adjustedMaxLength)
                .requireStepByStep(requireStepByStep)
                .requireCitations(requireCitations)
                .build();
    }

    private OutputContract buildOutputContract(PromptObjective objective, String jsonSchema) {
        if (objective == PromptObjective.EXTRACTION) {
            // 사용자 제공 스키마를 우선 사용, 없으면 제네릭 기본값 적용
            String schema = (jsonSchema != null && !jsonSchema.isBlank())
                    ? jsonSchema
                    : """
                      {
                        "type": "object",
                        "properties": {
                          "result": { "type": "string" }
                        },
                        "required": ["result"]
                      }
                      """;
            return OutputContract.jsonStructured(schema, 1000);
        }
        return OutputContract.freeText(2000);
    }

    private String buildInstructionContent(PromptObjective objective) {
        return switch (objective) {
            case FACTUAL ->
                    "Provide accurate, well-sourced information. Include citations or uncertainty markers where appropriate.";
            case REASONING ->
                    "Analyze the problem step-by-step. Show your reasoning process clearly at each stage.";
            case EXTRACTION ->
                    "Extract and structure the requested information strictly according to the specified output schema.";
            case PLANNING ->
                    "Create a structured, actionable plan with clear steps, dependencies, and success criteria.";
            case CREATIVE_WITH_CONSTRAINTS ->
                    "Generate creative content that fully respects the given constraints and requirements.";
            case ANALYTICAL ->
                    "Analyze the subject thoroughly, evaluating evidence from multiple perspectives. Compare and contrast key aspects with supporting rationale.";
        };
    }
}
