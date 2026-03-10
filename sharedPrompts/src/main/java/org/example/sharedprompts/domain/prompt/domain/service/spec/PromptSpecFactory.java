package org.example.sharedprompts.domain.prompt.domain.service.spec;

import org.example.sharedprompts.domain.prompt.application.semantic.resolution.SemanticResolutionService;
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
import org.example.sharedprompts.domain.prompt.common.guideline.context.RuleContext;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.example.sharedprompts.domain.prompt.domain.descriptor.RoleDescriptorPort;

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
    private final RoleDescriptorPort roleDescriptorPort;

    /**
     * 레거시/테스트 호환용 생성자.
     * 런타임 DI에서는 GuidelineBundleBuilder를 명시 주입하는 4-arg 생성자를 사용하세요.
     */
    @Deprecated(forRemoval = true)
    public PromptSpecFactory(ObjectiveRegistry objectiveRegistry,
                             StrategyBundlePolicy strategyBundlePolicy,
                             ObjectiveResolverPort objectiveResolver) {
        this(objectiveRegistry, strategyBundlePolicy, objectiveResolver, new GuidelineBundleBuilder(), null);
    }

    public PromptSpecFactory(ObjectiveRegistry objectiveRegistry,
                             StrategyBundlePolicy strategyBundlePolicy,
                             ObjectiveResolverPort objectiveResolver,
                             GuidelineBundleBuilder guidelineBundleBuilder) {
        this(objectiveRegistry, strategyBundlePolicy, objectiveResolver, guidelineBundleBuilder, null);
    }

    public PromptSpecFactory(ObjectiveRegistry objectiveRegistry,
                             StrategyBundlePolicy strategyBundlePolicy,
                             ObjectiveResolverPort objectiveResolver,
                             GuidelineBundleBuilder guidelineBundleBuilder,
                             RoleDescriptorPort roleDescriptorPort) {
        this.objectiveRegistry = Objects.requireNonNull(objectiveRegistry, "objectiveRegistry must not be null");
        this.strategyBundlePolicy = Objects.requireNonNull(strategyBundlePolicy, "strategyBundlePolicy must not be null");
        this.objectiveResolver = Objects.requireNonNull(objectiveResolver, "objectiveResolver must not be null");
        this.guidelineBundleBuilder = guidelineBundleBuilder != null ? guidelineBundleBuilder : new GuidelineBundleBuilder();
        this.roleDescriptorPort = roleDescriptorPort;
    }

    /**
     * V3 path blocked: production must use {@link #createFromConfirmedAxes(ConfirmedSemanticAxes, String, String)}.
     * This overload builds spec from taskDomain/objective/tone/style without category→intent mediation.
     *
     * @throws UnsupportedOperationException always
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
        throw new UnsupportedOperationException(
                "PromptSpecFactory.createForV3(...) without ConfirmedSemanticAxes is not supported. "
                        + "Use createFromConfirmedAxes(ConfirmedSemanticAxes, rawInput, jsonSchema).");
    }

    /**
     * Builds PromptSpec from confirmed semantic axes (category-aware pipeline).
     * Role and actionType are optional; when absent no role section is added.
     *
     * <p>This is the <b>canonical</b> path: all semantic meaning comes from {@code axes}; no inference from
     * category, task domain, or output contract. Use this when the caller has already resolved
     * PromptCategory → ActionIntent → RoleType/ActionType via {@link SemanticResolutionService}.
     */
    public PromptSpec createFromConfirmedAxes(
            ConfirmedSemanticAxes axes,
            String rawInput,
            String jsonSchema
    ) {
        if (axes == null || rawInput == null || rawInput.isBlank()) {
            throw new IllegalArgumentException("axes and rawInput are required.");
        }
        TaskDomain effectiveTaskDomain = axes.taskDomain() != null ? axes.taskDomain() : TaskDomain.GENERAL;
        PromptObjective objective = axes.objective();
        if (objective == null) {
            throw new IllegalArgumentException("axes.objective is required.");
        }
        ObjectiveProfile profile = objectiveRegistry.get(objective);
        ExperienceLevel level = axes.experienceLevel() != null ? axes.experienceLevel() : ExperienceLevel.INTERMEDIATE;
        Constraints constraints = profile.constraints(level);
        OutputContract outputContract = profile.outputContract(jsonSchema, constraints.getMaxLength());
        RuleContext ruleContext = RuleContext.of(
                effectiveTaskDomain,
                profile.objective().name(),
                axes.actionType().orElse(null),
                profile.supportsConstrainedDecoding(),
                outputContract.hasJsonSchema(),
                rawInput
        );
        List<PromptSection> sections = buildSections(
                profile,
                axes.role().orElse(null),
                effectiveTaskDomain,
                axes.language(),
                ruleContext
        );
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
                .useConstrainedDecoding(profile.supportsConstrainedDecoding())
                .contentSandbox(ContentSandbox.defaults())
                .role(axes.role().orElse(null))
                .tone(axes.tone())
                .style(axes.style())
                .strategyBundle(bundle)
                .locale(axes.language())
                .rawInput(rawInput)
                .actionType(axes.actionType().orElse(null))
                .build();
    }

    /**
     * Blocked: production must use {@link #createFromConfirmedAxes(ConfirmedSemanticAxes, String, String)}.
     * This overload derives objective from taskDomain+actionType only (no category→intent mediation).
     *
     * @throws UnsupportedOperationException always
     */
    @Deprecated(since = "semantic-pipeline", forRemoval = true)
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
        throw new UnsupportedOperationException(
                "PromptSpecFactory.create(...) without ConfirmedSemanticAxes is not supported. "
                        + "Use createFromConfirmedAxes(ConfirmedSemanticAxes, rawInput, jsonSchema).");
    }

    /**
     * Blocked: production must use {@link #createFromConfirmedAxes(ConfirmedSemanticAxes, String, String)}.
     *
     * @throws UnsupportedOperationException always
     */
    @Deprecated(since = "semantic-pipeline", forRemoval = true)
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
        throw new UnsupportedOperationException(
                "PromptSpecFactory.create(...) without ConfirmedSemanticAxes is not supported. "
                        + "Use createFromConfirmedAxes(ConfirmedSemanticAxes, rawInput, jsonSchema).");
    }

    /**
     * Blocked: production must use {@link #createFromConfirmedAxes(ConfirmedSemanticAxes, String, String)}.
     * Legacy: objective from taskDomain+actionType only (no category→intent mediation).
     *
     * @param jsonSchema EXTRACTION 시 사용할 JSON Schema (null이면 기본 스키마 사용)
     * @throws UnsupportedOperationException always
     */
    @Deprecated(since = "semantic-pipeline", forRemoval = true)
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
        throw new UnsupportedOperationException(
                "PromptSpecFactory.create(...) without ConfirmedSemanticAxes is not supported. "
                        + "Use createFromConfirmedAxes(ConfirmedSemanticAxes, rawInput, jsonSchema).");
    }

    /**
     * Blocked: production must use {@link #createFromConfirmedAxes(ConfirmedSemanticAxes, String, String)}.
     * Legacy: objective from {@link ObjectiveResolverPort#resolve(TaskDomain, ActionTypeInterface)}; no category→intent.
     *
     * @param jsonSchema        EXTRACTION 시 사용할 JSON Schema (null이면 기본 스키마 사용)
     * @param requiredKeywords  생성 결과에 반드시 포함되어야 할 키워드 목록 (null/empty 허용)
     * @param prohibitedKeywords 생성 결과에 포함되면 안 되는 키워드 목록 (null/empty 허용)
     * @throws UnsupportedOperationException always
     */
    @Deprecated(since = "semantic-pipeline", forRemoval = true)
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
        throw new UnsupportedOperationException(
                "PromptSpecFactory.create(...) without ConfirmedSemanticAxes is not supported. "
                        + "Use createFromConfirmedAxes(ConfirmedSemanticAxes, rawInput, jsonSchema).");
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
            LanguageType locale,
            RuleContext ruleContext
    ) {
        List<PromptSection> sections = new ArrayList<>();

        // 1) Role 섹션 (공통) — 로케일별 이름/설명은 RoleDescriptorPort에 위임
        if (role != null) {
            LanguageType effectiveLocale = locale != null ? locale : LanguageType.KOREAN;
            String roleName = roleDescriptorPort != null
                    ? roleDescriptorPort.getRoleName(role, effectiveLocale)
                    : role.getRoleNameByLang(effectiveLocale);
            String roleDesc = roleDescriptorPort != null
                    ? roleDescriptorPort.getDescription(role, effectiveLocale)
                    : role.getDescriptionByLang(effectiveLocale);
            sections.add(PromptSection.required(
                    PromptSection.SectionType.ROLE,
                    "You are a " + roleName + ". " + roleDesc
            ));
        }

        // 2) Checklist 섹션 — 단일 GuidelineBundle (규칙 중복 제거, 토큰 예산 적용)
        GuidelineBundle bundle = guidelineBundleBuilder.build(taskDomain, ruleContext);
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
