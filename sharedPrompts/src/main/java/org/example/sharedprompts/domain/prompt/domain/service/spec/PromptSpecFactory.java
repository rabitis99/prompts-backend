package org.example.sharedprompts.domain.prompt.domain.service.spec;

import org.example.sharedprompts.domain.prompt.domain.model.spec.Constraints;
import org.example.sharedprompts.domain.prompt.domain.model.spec.ContentSandbox;
import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSection;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.policy.strategy.StrategyBundlePolicy;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolverPort;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.strategy.PromptStrategyBundle;
import org.example.sharedprompts.domain.prompt.common.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.guideline.bundle.GuidelineBundle;
import org.example.sharedprompts.domain.prompt.common.guideline.bundle.GuidelineBundleBuilder;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private final GuidelineBundleBuilder guidelineBundleBuilder;

    public PromptSpecFactory(ObjectiveRegistry objectiveRegistry,
                             StrategyBundlePolicy strategyBundlePolicy,
                             ObjectiveResolverPort objectiveResolver) {
        this(objectiveRegistry, strategyBundlePolicy, objectiveResolver, new GuidelineBundleBuilder());
    }

    public PromptSpecFactory(ObjectiveRegistry objectiveRegistry,
                             StrategyBundlePolicy strategyBundlePolicy,
                             ObjectiveResolverPort objectiveResolver,
                             GuidelineBundleBuilder guidelineBundleBuilder) {
        this.objectiveRegistry = Objects.requireNonNull(objectiveRegistry, "objectiveRegistry must not be null");
        this.strategyBundlePolicy = Objects.requireNonNull(strategyBundlePolicy, "strategyBundlePolicy must not be null");
        this.objectiveResolver = Objects.requireNonNull(objectiveResolver, "objectiveResolver must not be null");
        this.guidelineBundleBuilder = guidelineBundleBuilder != null ? guidelineBundleBuilder : new GuidelineBundleBuilder();
    }

    /**
     * V3 전용 생성 경로 — Intent에서 이미 Objective를 해석한 경우 사용한다.
     */
    public PromptSpec createForV3(
            String rawInput,
            TaskDomain taskDomain,
            PromptObjective objective,
            ToneType tone,
            StyleType style,
            LanguageType locale,
            ExperienceLevel experienceLevel,
            String jsonSchema
    ) {
        if (rawInput == null || rawInput.isBlank()) {
            throw new IllegalArgumentException("rawInput은 null/blank일 수 없습니다.");
        }

        ExperienceLevel level = experienceLevel != null ? experienceLevel : ExperienceLevel.INTERMEDIATE;
        TaskDomain effectiveTaskDomain = taskDomain != null ? taskDomain : TaskDomain.GENERAL;

        ObjectiveProfile profile = objectiveRegistry.get(objective);

        Constraints constraints = profile.constraints(level);
        OutputContract outputContract = profile.outputContract(jsonSchema, constraints.getMaxLength());
        List<PromptSection> sections = buildSections(profile, null, effectiveTaskDomain, locale);
        PromptStrategyBundle bundle = strategyBundlePolicy.resolveBundle(objective, false);

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
                .role(null)
                .tone(tone != null ? tone : ToneType.NEUTRAL)
                .style(style != null ? style : StyleType.NARRATIVE)
                .strategyBundle(bundle)
                .locale(locale != null ? locale : LanguageType.KOREAN)
                .rawInput(rawInput)
                .actionType(null)
                .build();
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
        return create(rawInput, taskDomain, actionType, role, tone, style, locale,
                experimentalEnabled, experienceLevel, jsonSchema, null, null);
    }

    /**
     * 전체 파라미터 + 키워드 제약 버전.
     *
     * @param jsonSchema        EXTRACTION 시 사용할 JSON Schema (null이면 기본 스키마 사용)
     * @param requiredKeywords  생성 결과에 반드시 포함되어야 할 키워드 목록 (null/empty 허용)
     * @param prohibitedKeywords 생성 결과에 포함되면 안 되는 키워드 목록 (null/empty 허용)
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
            String jsonSchema,
            List<String> requiredKeywords,
            List<String> prohibitedKeywords
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
        Constraints baseConstraints = profile.constraints(level);
        Constraints constraints = enrichConstraintsWithKeywords(
                baseConstraints,
                requiredKeywords,
                prohibitedKeywords
        );
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

    private Constraints enrichConstraintsWithKeywords(Constraints base,
                                                      List<String> requiredKeywords,
                                                      List<String> prohibitedKeywords) {
        boolean hasRequired = requiredKeywords != null && !requiredKeywords.isEmpty();
        boolean hasProhibited = prohibitedKeywords != null && !prohibitedKeywords.isEmpty();
        if (!hasRequired && !hasProhibited) {
            return base;
        }

        Constraints.Builder builder = Constraints.builder()
                .minLength(base.getMinLength())
                .maxLength(base.getMaxLength())
                .requireStepByStep(base.isRequireStepByStep())
                .requireCitations(base.isRequireCitations());

        builder.requiredKeywords(hasRequired ? requiredKeywords : base.getRequiredKeywords());
        builder.prohibitedKeywords(hasProhibited ? prohibitedKeywords : base.getProhibitedKeywords());

        return builder.build();
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

        // 2) Checklist 섹션 — 단일 GuidelineBundle (규칙 중복 제거, 토큰 예산 적용)
        GuidelineBundle bundle = guidelineBundleBuilder.build(taskDomain);
        StringBuilder checklistBuilder = new StringBuilder();
        appendGuidelineRules(checklistBuilder, bundle.hardRules(), locale);
        appendGuidelineRules(checklistBuilder, bundle.softRules(), locale);
        if (!checklistBuilder.isEmpty()) {
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
        List<PromptSection> extra = profile.extraSections();
        if (extra != null) {
            sections.addAll(extra);
        }

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
