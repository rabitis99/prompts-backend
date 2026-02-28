package org.example.sharedprompts.domain.prompt.domain.service;

import org.example.sharedprompts.domain.prompt.domain.model.Constraints;
import org.example.sharedprompts.domain.prompt.domain.model.ContentSandbox;
import org.example.sharedprompts.domain.prompt.domain.model.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSection;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.policy.StrategyBundlePolicy;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolverPort;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.PromptStrategyBundle;
import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.guideline.GuidelineRule;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 입력을 받아 {@link PromptSpec}을 생성하는 도메인 팩토리.
 *
 * <p>Spring 의존 없음 — {@code PromptDomainConfig}에서 생성·주입한다.
 *
 * <p>Objective별 분기(priority, rubric, constraints, sections 등)는
 * {@link ObjectiveRegistry}를 통해 조회한 {@link ObjectiveProfile}에 위임한다.
 * switch/case 없음, 새 Objective 추가 시 이 클래스 수정 불필요(OCP).
 */
public class PromptSpecFactory {

    private final ObjectiveRegistry objectiveRegistry;
    private final StrategyBundlePolicy strategyBundlePolicy;
    private final ObjectiveResolverPort objectiveResolver;

    public PromptSpecFactory(ObjectiveRegistry objectiveRegistry,
                             StrategyBundlePolicy strategyBundlePolicy,
                             ObjectiveResolverPort objectiveResolver) {
        this.objectiveRegistry = objectiveRegistry;
        this.strategyBundlePolicy = strategyBundlePolicy;
        this.objectiveResolver = objectiveResolver;
    }

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
        if (rawInput == null || rawInput.isBlank()) {
            throw new IllegalArgumentException("rawInput은 null/blank일 수 없습니다.");
        }

        ExperienceLevel level = experienceLevel != null ? experienceLevel : ExperienceLevel.INTERMEDIATE;
        TaskDomain effectiveTaskDomain = taskDomain != null ? taskDomain : TaskDomain.GENERAL;

        // ── Objective 결정 ──────────────────────────────────────────────────
        PromptObjective objective = objectiveResolver.resolve(effectiveTaskDomain, actionType);
        ObjectiveProfile profile = objectiveRegistry.get(objective);

        // ── Profile에서 Objective별 항목 조회 (switch/case 없음) ────────────
        Constraints constraints = profile.constraints(level);
        OutputContract outputContract = profile.outputContract(jsonSchema, constraints.getMaxLength());
        List<PromptSection> sections = buildSections(profile, role, effectiveTaskDomain, locale);
        PromptStrategyBundle bundle = strategyBundlePolicy.resolveBundle(objective, experimentalEnabled);

        return PromptSpec.builder()
                .objective(objective)
                .priority(profile.priority())
                .rubric(profile.rubric())
                .taskDomain(effectiveTaskDomain)
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

    // ── 섹션 빌드 ─────────────────────────────────────────────────────────

    private List<PromptSection> buildSections(
            ObjectiveProfile profile,
            RoleTypeInterface role,
            TaskDomain taskDomain,
            LanguageType locale
    ) {
        List<PromptSection> sections = new ArrayList<>();

        // 1) Role 섹션 (공통) — 로케일별 이름/설명은 RoleTypeInterface default 메서드에 위임
        if (role != null) {
            LanguageType effectiveLocale = locale != null ? locale : LanguageType.KOREAN;
            String roleName = role.getRoleNameByLang(effectiveLocale);
            String roleDesc = role.getDescriptionByLang(effectiveLocale);
            sections.add(PromptSection.required(
                    PromptSection.SectionType.ROLE,
                    "You are a " + roleName + ". " + roleDesc
            ));
        }

        // 2) Checklist 섹션 — TaskDomain GuidelinePolicy (공통)
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

        // 3) Instruction 섹션 — Objective별 지시 방향 (profile 위임)
        sections.add(PromptSection.required(
                PromptSection.SectionType.INSTRUCTION,
                profile.instructionContent(locale)
        ));

        // 4) Objective별 추가 섹션 (OUTPUT_FORMAT, CONSTRAINTS 등)
        sections.addAll(profile.extraSections());

        return sections;
    }

    private void appendGuidelineRules(StringBuilder sb, List<GuidelineRule> rules, LanguageType locale) {
        if (rules == null || rules.isEmpty()) return;
        for (GuidelineRule rule : rules) {
            String description = switch (locale != null ? locale : LanguageType.KOREAN) {
                case ENGLISH -> rule.description().en();
                case JAPANESE -> rule.description().ja();
                default -> rule.description().ko();
            };
            sb.append("- ").append(description).append("\n");
        }
    }
}
