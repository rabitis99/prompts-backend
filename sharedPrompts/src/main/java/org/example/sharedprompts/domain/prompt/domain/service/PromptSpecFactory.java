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
        PromptObjective objective = resolveObjective(taskDomain, actionType);
        QualityPriority priority = resolvePriority(objective);
        QualityRubric rubric = buildRubric(objective);
        List<PromptSection> sections = buildSections(objective, role, taskDomain, locale);
        Constraints constraints = buildConstraints(objective);
        OutputContract outputContract = buildOutputContract(objective);
        PromptStrategyBundle bundle = strategyBundlePolicy.resolveBundle(objective, experimentalEnabled);

        return PromptSpec.builder()
                .objective(objective)
                .priority(priority)
                .rubric(rubric)
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
     * 사용자에게는 노출하지 않는 내부 매핑이다.
     */
    PromptObjective resolveObjective(TaskDomain taskDomain, ActionTypeInterface actionType) {
        if (taskDomain == null) return PromptObjective.REASONING;

        return switch (taskDomain) {
            case TECHNICAL -> PromptObjective.REASONING;
            case ANALYTICAL -> PromptObjective.FACTUAL;
            case CREATIVE -> PromptObjective.CREATIVE_WITH_CONSTRAINTS;
            case PRACTICAL -> PromptObjective.PLANNING;
            case EDUCATIONAL -> PromptObjective.REASONING;
            default -> PromptObjective.REASONING;
        };
    }

    private QualityPriority resolvePriority(PromptObjective objective) {
        return switch (objective) {
            case FACTUAL -> QualityPriority.ACCURACY_FIRST;
            case EXTRACTION -> QualityPriority.STRUCTURE_FIRST;
            case CREATIVE_WITH_CONSTRAINTS -> QualityPriority.CREATIVITY_SECOND;
            default -> QualityPriority.ACCURACY_FIRST;
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

        // Objective별 추가 구조 섹션 (내용은 Renderer에서 채워질 수 있도록 비워 둔다)
        sections.add(PromptSection.required(PromptSection.SectionType.INSTRUCTION, ""));

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

    private Constraints buildConstraints(PromptObjective objective) {
        return switch (objective) {
            case FACTUAL -> Constraints.builder()
                    .maxLength(3000)
                    .requireCitations(true)
                    .build();
            case REASONING -> Constraints.builder()
                    .maxLength(2500)
                    .requireStepByStep(true)
                    .build();
            case EXTRACTION -> Constraints.builder()
                    .maxLength(1000)
                    .build();
            case PLANNING -> Constraints.builder()
                    .maxLength(3000)
                    .requireStepByStep(true)
                    .build();
            case CREATIVE_WITH_CONSTRAINTS -> Constraints.builder()
                    .maxLength(2000)
                    .build();
        };
    }

    private OutputContract buildOutputContract(PromptObjective objective) {
        if (objective == PromptObjective.EXTRACTION) {
            // EXTRACTION: JSON Schema 기반 출력 계약 (실제 스키마는 입력에서 추출하거나 기본값 사용)
            return OutputContract.jsonStructured(
                    """
                    {
                      "type": "object",
                      "properties": {
                        "result": { "type": "string" }
                      },
                      "required": ["result"]
                    }
                    """,
                    1000
            );
        }
        return OutputContract.freeText(2000);
    }
}
