package org.example.sharedprompts.domain.prompt.domain.semantic.impl;

import org.example.sharedprompts.domain.prompt.common.enums.action.category.analysis.AnalysisActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.business.BusinessActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.business.CustomerSupportActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.creative.CreativeActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.design.DesignActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.CodingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.DevelopmentActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.education.EducationActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.marketing.MarketingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity.ProductivityActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.research.ResearchActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.writing.WritingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.business.BusinessRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.business.CustomerSupportRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.creative.CreativeRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.design.DesignRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.development.DevelopmentRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.etc.EtcRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.marketing.MarketingRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.productivity.ProductivityRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.research.ResearchRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.education.EducationRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.writing.WritingRoleType;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.FallbackCandidate;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticFitLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
/**
 * In-memory registry of category semantic profiles for all {@link PromptCategory} values.
 *
 * <p><b>EXTRACTION is an intentional special case, not an omission.</b> {@link PromptCategory#EXTRACTION}
 * does NOT use {@link CategorySemanticProfile}. It is resolved only via
 * {@link org.example.sharedprompts.domain.prompt.application.semantic.resolution.GenerationSemanticResolver#resolveExtraction},
 * which bypasses this registry and applies a fixed semantic axis (intent=EXTRACT, objective=EXTRACTION).
 * Do not add EXTRACTION to this registry.</p>
 *
 * <p>DESIGN: create, review, critique, explain, plan branches.
 * DEVELOPMENT: implement, debug, explain, refactor, review, architect.
 * WRITING: write, rewrite, edit, structure, polish, clarify.
 * RESEARCH: investigate, summarize, compare, synthesize, explain.
 * BUSINESS: plan, decide, analyze, propose, optimize.
 * CONTENT: create, adapt, outline, repurpose, platform-fit.
 *
 * <p>Categories that use the full constructor can specify intent fit levels (PREFERRED/ALLOWED/DISCOURAGED)
 * and discouraged tones/styles. Those that pass empty fit levels and null for discouraged get
 * "all allowed intents treated as ALLOWED" with no tone/style restrictions.
 * <p>Legacy categories (SALES, OPERATIONS, CONTENT_CREATION, ANALYSIS) are not registered; {@link #getProfile(PromptCategory)}
 * uses {@link PromptCategory#canonical()} so they resolve to MARKETING, BUSINESS, WRITING, DATA_ANALYSIS respectively.</p>
 *
 * <p>Must be constructed with both {@link CanonicalActionRegistry} and {@link ActionTypeRegistry} so that
 * compatible actions are derived from groups (group-first). Wired via config bean, not component scan.</p>
 */
public class DefaultCategorySemanticProfileRegistry implements CategorySemanticProfileRegistry {

    private final Map<PromptCategory, CategorySemanticProfile> profiles;
    private final CanonicalActionRegistry canonicalActionRegistry;
    private final ActionTypeRegistry actionTypeRegistry;

    /**
     * Group-first construction: compatible ActionGroups are primary; compatible ActionTypes
     * are derived from registry by group membership. Requires both registries so that
     * action lists are always derived from groups for scalability (600+ ActionTypes).
     */
    public DefaultCategorySemanticProfileRegistry(CanonicalActionRegistry canonicalActionRegistry,
                                                   ActionTypeRegistry actionTypeRegistry) {
        this.canonicalActionRegistry = Objects.requireNonNull(canonicalActionRegistry, "canonicalActionRegistry");
        this.actionTypeRegistry = Objects.requireNonNull(actionTypeRegistry, "actionTypeRegistry");
        Map<PromptCategory, CategorySemanticProfile> map = new HashMap<>();
        registerDesign(map);
        registerDevelopment(map);
        registerWriting(map);
        registerResearch(map);
        registerBusiness(map);
        registerProductivity(map);
        registerDataAnalysis(map);
        registerMarketing(map);
        registerCustomerSupport(map);
        registerCreative(map);
        registerLegal(map);
        registerEducation(map);
        registerEtc(map);
        this.profiles = Collections.unmodifiableMap(map);
    }

    @Override
    public Optional<CategorySemanticProfile> getProfile(PromptCategory category) {
        if (category == null) {
            return Optional.empty();
        }
        // Semantic resolution uses canonical category; legacy (SALES, OPERATIONS, CONTENT_CREATION, ANALYSIS) resolve to same profile as canonical.
        return Optional.ofNullable(profiles.get(category.canonical()));
    }

    /** Builds action group capability map from concrete actions; primary internal capability layer. */
    private Map<ActionIntent, List<ActionGroup>> toActionGroupMap(Map<ActionIntent, List<ActionTypeInterface>> actions) {
        if (actions == null || actions.isEmpty()) return Map.of();
        Map<ActionIntent, List<ActionGroup>> out = new HashMap<>();
        for (Map.Entry<ActionIntent, List<ActionTypeInterface>> e : actions.entrySet()) {
            List<ActionGroup> groups = e.getValue().stream()
                    .map(canonicalActionRegistry::toCanonical)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .distinct()
                    .toList();
            if (!groups.isEmpty()) out.put(e.getKey(), groups);
        }
        return out;
    }

    /**
     * Group-first: builds group map from actions, then derives action list from registry when available.
     * When actionTypeRegistry is non-null, returned actions are all registry actions whose group is in the map.
     */
    private GroupMapAndActions prepare(Map<ActionIntent, List<ActionTypeInterface>> actions) {
        Map<ActionIntent, List<ActionGroup>> groupMap = toActionGroupMap(actions);
        Map<ActionIntent, List<ActionTypeInterface>> toUse = actionTypeRegistry != null
                ? deriveActionsFromGroups(groupMap, actions, actionTypeRegistry.getAll())
                : actions;
        return new GroupMapAndActions(groupMap, toUse);
    }

    private record GroupMapAndActions(Map<ActionIntent, List<ActionGroup>> groupMap,
                                      Map<ActionIntent, List<ActionTypeInterface>> actions) {}

    /**
     * Derives compatible action list: seed actions in original order first, then same-group
     * expansion from registry. Preserves recommendation contract (order = preference).
     */
    private Map<ActionIntent, List<ActionTypeInterface>> deriveActionsFromGroups(
            Map<ActionIntent, List<ActionGroup>> groupMap,
            Map<ActionIntent, List<ActionTypeInterface>> seedActions,
            Collection<ActionTypeInterface> allActions) {
        if (groupMap == null || groupMap.isEmpty() || allActions == null) return Map.of();
        Map<ActionIntent, List<ActionTypeInterface>> out = new HashMap<>();
        for (Map.Entry<ActionIntent, List<ActionGroup>> e : groupMap.entrySet()) {
            Set<ActionGroup> allowedSet = new HashSet<>(e.getValue());
            List<ActionTypeInterface> seedList = seedActions != null ? seedActions.getOrDefault(e.getKey(), List.of()) : List.of();
            List<ActionTypeInterface> result = new ArrayList<>();
            Set<ActionTypeInterface> added = new HashSet<>();
            for (ActionTypeInterface a : seedList) {
                if (a != null && a.getActionGroup() != null && allowedSet.contains(a.getActionGroup()) && added.add(a)) {
                    result.add(a);
                }
            }
            for (ActionTypeInterface a : allActions) {
                if (a.getActionGroup() != null && allowedSet.contains(a.getActionGroup()) && added.add(a)) {
                    result.add(a);
                }
            }
            if (!result.isEmpty()) out.put(e.getKey(), result);
        }
        return out;
    }

    private void registerDesign(Map<PromptCategory, CategorySemanticProfile> target) {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.GENERATE, List.of(DesignRoleType.GRAPHIC_DESIGNER, DesignRoleType.UI_UX_DESIGNER, DesignRoleType.PRODUCT_DESIGNER));
        roles.put(ActionIntent.CREATE, List.of(DesignRoleType.UI_UX_DESIGNER, DesignRoleType.GRAPHIC_DESIGNER));
        roles.put(ActionIntent.EVALUATE, List.of(DesignRoleType.UI_UX_DESIGNER, DesignRoleType.PRODUCT_DESIGNER));
        roles.put(ActionIntent.CRITIQUE, List.of(DesignRoleType.UI_UX_DESIGNER, DesignRoleType.PRODUCT_DESIGNER));
        roles.put(ActionIntent.EXPLAIN, List.of(DesignRoleType.UI_UX_DESIGNER, DesignRoleType.INTERACTION_DESIGNER));
        roles.put(ActionIntent.PLAN, List.of(DesignRoleType.PRODUCT_DESIGNER, DesignRoleType.UI_UX_DESIGNER));
        roles.put(ActionIntent.REWRITE, List.of(DesignRoleType.GRAPHIC_DESIGNER, DesignRoleType.UI_UX_DESIGNER));
        roles.put(ActionIntent.RECOMMEND, List.of(DesignRoleType.PRODUCT_DESIGNER, DesignRoleType.UI_UX_DESIGNER));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.GENERATE, List.of(DesignActionType.GRAPHIC_DESIGN, DesignActionType.UI_DESIGN, DesignActionType.VISUAL_IDENTITY));
        actions.put(ActionIntent.CREATE, List.of(DesignActionType.UI_DESIGN, DesignActionType.UX_DESIGN, DesignActionType.WIREFRAMING, DesignActionType.PROTOTYPING));
        actions.put(ActionIntent.EVALUATE, List.of(DesignActionType.UI_DESIGN, DesignActionType.UX_DESIGN, DesignActionType.PRODUCT_DESIGN));
        actions.put(ActionIntent.CRITIQUE, List.of(DesignActionType.UI_DESIGN, DesignActionType.UX_DESIGN, DesignActionType.PRODUCT_DESIGN));
        actions.put(ActionIntent.EXPLAIN, List.of(DesignActionType.DESIGN_DOC, DesignActionType.UI_DESIGN, DesignActionType.UX_DESIGN));
        actions.put(ActionIntent.PLAN, List.of(DesignActionType.WIREFRAMING, DesignActionType.PRODUCT_DESIGN, DesignActionType.DESIGN_DOC));
        actions.put(ActionIntent.REWRITE, List.of(DesignActionType.GRAPHIC_DESIGN, DesignActionType.VISUAL_IDENTITY));
        actions.put(ActionIntent.RECOMMEND, List.of(DesignActionType.PRODUCT_DESIGN, DesignActionType.DESIGN_DOC));

        Set<ActionIntent> allowed = Set.of(ActionIntent.GENERATE, ActionIntent.CREATE, ActionIntent.EVALUATE, ActionIntent.CRITIQUE, ActionIntent.EXPLAIN, ActionIntent.PLAN, ActionIntent.REWRITE, ActionIntent.RECOMMEND, ActionIntent.DIAGNOSE);
        Map<ActionIntent, SemanticFitLevel> fitLevels = new HashMap<>();
        fitLevels.put(ActionIntent.CREATE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.EVALUATE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.CRITIQUE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.EXPLAIN, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.GENERATE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.PLAN, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.REWRITE, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.RECOMMEND, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.DIAGNOSE, SemanticFitLevel.DISCOURAGED); // only in narrow critique/debug context

        GroupMapAndActions prepared = prepare(actions);
        target.put(PromptCategory.DESIGN, new DefaultCategorySemanticProfile(
                PromptCategory.DESIGN, TaskDomain.CREATIVE, allowed, fitLevels, roles, prepared.actions(),
                null, null, ActionIntent.CREATE,
                List.of(new FallbackCandidate(ActionIntent.CREATE, "Design typically starts with creating or generating; CREATE is the default when intent is unspecified.")),
                prepared.groupMap()));
    }

    private void registerDevelopment(Map<PromptCategory, CategorySemanticProfile> target) {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.GENERATE, List.of(DevelopmentRoleType.FULL_STACK_DEVELOPER, DevelopmentRoleType.BACKEND_DEVELOPER, DevelopmentRoleType.FRONTEND_DEVELOPER));
        roles.put(ActionIntent.DIAGNOSE, List.of(DevelopmentRoleType.BACKEND_DEVELOPER, DevelopmentRoleType.FRONTEND_DEVELOPER));
        roles.put(ActionIntent.EXPLAIN, List.of(DevelopmentRoleType.FULL_STACK_DEVELOPER, DevelopmentRoleType.CLOUD_ARCHITECT));
        roles.put(ActionIntent.EVALUATE, List.of(DevelopmentRoleType.FULL_STACK_DEVELOPER, DevelopmentRoleType.BACKEND_DEVELOPER));
        roles.put(ActionIntent.IMPROVE, List.of(DevelopmentRoleType.BACKEND_DEVELOPER, DevelopmentRoleType.FRONTEND_DEVELOPER));
        roles.put(ActionIntent.PLAN, List.of(DevelopmentRoleType.CLOUD_ARCHITECT, DevelopmentRoleType.FULL_STACK_DEVELOPER));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.GENERATE, List.of(CodingActionType.CODE_GENERATION, CodingActionType.CODE_MODIFICATION, CodingActionType.TEST_GENERATION));
        actions.put(ActionIntent.DIAGNOSE, List.of(CodingActionType.DEBUGGING, CodingActionType.CODE_ANALYSIS));
        actions.put(ActionIntent.EXPLAIN, List.of(DevelopmentActionType.DOCUMENTATION, DevelopmentActionType.CODEBASE_ANALYSIS));
        actions.put(ActionIntent.EVALUATE, List.of(CodingActionType.CODE_REVIEW, DevelopmentActionType.CODEBASE_ANALYSIS));
        actions.put(ActionIntent.IMPROVE, List.of(CodingActionType.CODE_MODIFICATION, DevelopmentActionType.CODEBASE_ANALYSIS));
        actions.put(ActionIntent.PLAN, List.of(DevelopmentActionType.ARCHITECTURE_DESIGN, DevelopmentActionType.SYSTEM_DESIGN));

        Set<ActionIntent> allowed = Set.of(ActionIntent.GENERATE, ActionIntent.DIAGNOSE, ActionIntent.EXPLAIN, ActionIntent.EVALUATE, ActionIntent.PLAN, ActionIntent.REFINE, ActionIntent.IMPROVE);
        Map<ActionIntent, SemanticFitLevel> fitLevels = new HashMap<>();
        fitLevels.put(ActionIntent.GENERATE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.DIAGNOSE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.EXPLAIN, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.EVALUATE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.IMPROVE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.PLAN, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.REFINE, SemanticFitLevel.ALLOWED);

        Map<ActionIntent, List<StyleType>> discouragedStyles = new HashMap<>();
        discouragedStyles.put(ActionIntent.GENERATE, List.of(StyleType.STORYTELLING, StyleType.CREATIVE));
        discouragedStyles.put(ActionIntent.DIAGNOSE, List.of(StyleType.STORYTELLING));
        discouragedStyles.put(ActionIntent.EXPLAIN, List.of(StyleType.STORYTELLING));

        GroupMapAndActions prepared = prepare(actions);
        target.put(PromptCategory.DEVELOPMENT, new DefaultCategorySemanticProfile(
                PromptCategory.DEVELOPMENT, TaskDomain.TECHNICAL, allowed, fitLevels, roles, prepared.actions(),
                null, discouragedStyles, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Development default: generate code or artifacts; override with intent for debug, explain, or plan.")),
                prepared.groupMap()));
    }

    private void registerWriting(Map<PromptCategory, CategorySemanticProfile> target) {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.CREATE, List.of(WritingRoleType.CONTENT_WRITER, WritingRoleType.COPYWRITER));
        roles.put(ActionIntent.GENERATE, List.of(WritingRoleType.COPYWRITER, WritingRoleType.CONTENT_WRITER));
        roles.put(ActionIntent.REWRITE, List.of(WritingRoleType.EDITOR, WritingRoleType.COPYWRITER));
        roles.put(ActionIntent.EDIT, List.of(WritingRoleType.EDITOR, WritingRoleType.CONTENT_WRITER));
        roles.put(ActionIntent.REFINE, List.of(WritingRoleType.EDITOR, WritingRoleType.COPYWRITER));
        roles.put(ActionIntent.IMPROVE, List.of(WritingRoleType.EDITOR, WritingRoleType.CONTENT_WRITER));
        roles.put(ActionIntent.OUTLINE, List.of(WritingRoleType.CONTENT_WRITER, WritingRoleType.EDITOR));
        roles.put(ActionIntent.SUMMARIZE, List.of(WritingRoleType.EDITOR, WritingRoleType.CONTENT_WRITER));
        roles.put(ActionIntent.EXPLAIN, List.of(WritingRoleType.TECHNICAL_WRITER, WritingRoleType.CONTENT_WRITER));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.CREATE, List.of(WritingActionType.ARTICLE_WRITING, WritingActionType.CREATIVE_WRITING_GEN));
        actions.put(ActionIntent.GENERATE, List.of(WritingActionType.ARTICLE_WRITING, WritingActionType.CREATIVE_WRITING_GEN, WritingActionType.COPYWRITING));
        actions.put(ActionIntent.REWRITE, List.of(WritingActionType.EDITING, WritingActionType.PROOFREADING, WritingActionType.DOC_UPDATE));
        actions.put(ActionIntent.EDIT, List.of(WritingActionType.EDITING, WritingActionType.PROOFREADING));
        actions.put(ActionIntent.REFINE, List.of(WritingActionType.EDITING, WritingActionType.DOC_UPDATE));
        actions.put(ActionIntent.IMPROVE, List.of(WritingActionType.EDITING, WritingActionType.DOC_UPDATE));
        actions.put(ActionIntent.OUTLINE, List.of(WritingActionType.ARTICLE_WRITING));
        actions.put(ActionIntent.SUMMARIZE, List.of(WritingActionType.EDITING, WritingActionType.ARTICLE_WRITING));
        actions.put(ActionIntent.EXPLAIN, List.of(WritingActionType.TECHNICAL_WRITING, WritingActionType.DOC_UPDATE));

        Set<ActionIntent> allowed = Set.of(ActionIntent.CREATE, ActionIntent.GENERATE, ActionIntent.REWRITE, ActionIntent.EDIT, ActionIntent.REFINE, ActionIntent.IMPROVE, ActionIntent.OUTLINE, ActionIntent.SUMMARIZE, ActionIntent.EXPLAIN, ActionIntent.PLAN);
        Map<ActionIntent, SemanticFitLevel> fitLevels = new HashMap<>();
        fitLevels.put(ActionIntent.CREATE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.REWRITE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.EDIT, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.IMPROVE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.OUTLINE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.GENERATE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.REFINE, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.SUMMARIZE, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.EXPLAIN, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.PLAN, SemanticFitLevel.ALLOWED);

        GroupMapAndActions prepared = prepare(actions);
        target.put(PromptCategory.WRITING, new DefaultCategorySemanticProfile(
                PromptCategory.WRITING, TaskDomain.CREATIVE, allowed, fitLevels, roles, prepared.actions(),
                null, null, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Writing default: generate new content; use REWRITE/EDIT/REFINE/IMPROVE for existing text.")),
                prepared.groupMap()));
    }

    private void registerResearch(Map<PromptCategory, CategorySemanticProfile> target) {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.INVESTIGATE, List.of(ResearchRoleType.RESEARCHER, ResearchRoleType.RESEARCH_METHODOLOGIST));
        roles.put(ActionIntent.ANALYZE, List.of(ResearchRoleType.RESEARCHER, ResearchRoleType.RESEARCH_METHODOLOGIST));
        roles.put(ActionIntent.SYNTHESIZE, List.of(ResearchRoleType.RESEARCHER, ResearchRoleType.ACADEMIC_WRITER));
        roles.put(ActionIntent.EXPLAIN, List.of(ResearchRoleType.RESEARCH_METHODOLOGIST, ResearchRoleType.RESEARCHER));
        roles.put(ActionIntent.SUMMARIZE, List.of(ResearchRoleType.RESEARCHER, ResearchRoleType.ACADEMIC_WRITER));
        roles.put(ActionIntent.GENERATE, List.of(ResearchRoleType.ACADEMIC_WRITER, ResearchRoleType.RESEARCHER));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.INVESTIGATE, List.of(ResearchActionType.DATA_INTERPRETATION, ResearchActionType.LITERATURE_REVIEW));
        actions.put(ActionIntent.ANALYZE, List.of(ResearchActionType.DATA_INTERPRETATION, ResearchActionType.LITERATURE_REVIEW, ResearchActionType.STATISTICAL_MODELING));
        actions.put(ActionIntent.SYNTHESIZE, List.of(ResearchActionType.LITERATURE_REVIEW, ResearchActionType.STATISTICAL_MODELING));
        actions.put(ActionIntent.SUMMARIZE, List.of(ResearchActionType.LITERATURE_REVIEW, ResearchActionType.PAPER_WRITING));
        actions.put(ActionIntent.EXPLAIN, List.of(ResearchActionType.RESEARCH_DESIGN, ResearchActionType.METHODOLOGY_DEVELOPMENT));
        actions.put(ActionIntent.GENERATE, List.of(ResearchActionType.PAPER_WRITING, ResearchActionType.HYPOTHESIS_FORMULATION));

        Set<ActionIntent> allowed = Set.of(ActionIntent.INVESTIGATE, ActionIntent.ANALYZE, ActionIntent.SYNTHESIZE, ActionIntent.EXPLAIN, ActionIntent.SUMMARIZE, ActionIntent.GENERATE, ActionIntent.EVALUATE, ActionIntent.EXTRACT);
        Map<ActionIntent, SemanticFitLevel> fitLevels = new HashMap<>();
        fitLevels.put(ActionIntent.INVESTIGATE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.ANALYZE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.SYNTHESIZE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.EXPLAIN, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.SUMMARIZE, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.GENERATE, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.EVALUATE, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.EXTRACT, SemanticFitLevel.ALLOWED);

        Map<ActionIntent, List<ToneType>> discouragedTones = new HashMap<>();
        discouragedTones.put(ActionIntent.ANALYZE, List.of(ToneType.MOTIVATIONAL, ToneType.ENTHUSIASTIC));
        discouragedTones.put(ActionIntent.SYNTHESIZE, List.of(ToneType.MOTIVATIONAL, ToneType.ENTHUSIASTIC));
        discouragedTones.put(ActionIntent.INVESTIGATE, List.of(ToneType.MOTIVATIONAL));

        GroupMapAndActions prepared = prepare(actions);
        target.put(PromptCategory.RESEARCH, new DefaultCategorySemanticProfile(
                PromptCategory.RESEARCH, TaskDomain.ANALYTICAL, allowed, fitLevels, roles, prepared.actions(),
                discouragedTones, null, ActionIntent.ANALYZE,
                List.of(new FallbackCandidate(ActionIntent.ANALYZE, "Research default: analyze or investigate; use SYNTHESIZE for literature/evidence synthesis.")),
                prepared.groupMap()));
    }

    private void registerBusiness(Map<PromptCategory, CategorySemanticProfile> target) {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.PLAN, List.of(BusinessRoleType.BUSINESS_CONSULTANT, BusinessRoleType.PROJECT_MANAGER));
        roles.put(ActionIntent.PROPOSE, List.of(BusinessRoleType.BUSINESS_CONSULTANT, BusinessRoleType.PROJECT_MANAGER));
        roles.put(ActionIntent.DECIDE, List.of(BusinessRoleType.BUSINESS_CONSULTANT, BusinessRoleType.BUSINESS_ANALYST_BUSINESS));
        roles.put(ActionIntent.ANALYZE, List.of(BusinessRoleType.BUSINESS_ANALYST_BUSINESS, BusinessRoleType.FINANCIAL_ANALYST));
        roles.put(ActionIntent.GENERATE, List.of(BusinessRoleType.PROJECT_MANAGER, BusinessRoleType.BUSINESS_ANALYST_BUSINESS));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.PLAN, List.of(BusinessActionType.BUSINESS_PLAN_DEVELOPMENT, BusinessActionType.PROJECT_MANAGEMENT, BusinessActionType.BUSINESS_STRATEGY));
        actions.put(ActionIntent.PROPOSE, List.of(BusinessActionType.PROPOSAL_WRITING, BusinessActionType.BUSINESS_STRATEGY));
        actions.put(ActionIntent.DECIDE, List.of(BusinessActionType.RISK_ASSESSMENT, BusinessActionType.CONTRACT_REVIEW));
        actions.put(ActionIntent.ANALYZE, List.of(BusinessActionType.FINANCIAL_ANALYSIS, BusinessActionType.RISK_ASSESSMENT));
        actions.put(ActionIntent.GENERATE, List.of(BusinessActionType.PROPOSAL_WRITING, BusinessActionType.REPORT_WRITING, BusinessActionType.PRESENTATION_PREPARATION));

        Set<ActionIntent> allowed = Set.of(ActionIntent.PLAN, ActionIntent.PROPOSE, ActionIntent.DECIDE, ActionIntent.ANALYZE, ActionIntent.GENERATE, ActionIntent.SUMMARIZE);
        Map<ActionIntent, SemanticFitLevel> fitLevels = new HashMap<>();
        fitLevels.put(ActionIntent.PROPOSE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.PLAN, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.DECIDE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.ANALYZE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.GENERATE, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.SUMMARIZE, SemanticFitLevel.ALLOWED);

        GroupMapAndActions prepared = prepare(actions);
        target.put(PromptCategory.BUSINESS, new DefaultCategorySemanticProfile(
                PromptCategory.BUSINESS, TaskDomain.PRACTICAL, allowed, fitLevels, roles, prepared.actions(),
                null, null, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Business default: generate proposals/reports; use PLAN/PROPOSE/DECIDE for strategy and decisions.")),
                prepared.groupMap()));
    }

    private void registerProductivity(Map<PromptCategory, CategorySemanticProfile> target) {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.PLAN, List.of(ProductivityRoleType.PRODUCTIVITY_EXPERT, ProductivityRoleType.TIME_MANAGEMENT_SPECIALIST));
        roles.put(ActionIntent.GENERATE, List.of(ProductivityRoleType.PRODUCTIVITY_EXPERT));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.PLAN, List.of(ProductivityActionType.SCHEDULE_PLANNING, ProductivityActionType.TASK_AUTOMATION));

        Set<ActionIntent> allowed = Set.of(ActionIntent.PLAN, ActionIntent.GENERATE, ActionIntent.ANALYZE);
        GroupMapAndActions prepared = prepare(actions);
        target.put(PromptCategory.PRODUCTIVITY, new DefaultCategorySemanticProfile(
                PromptCategory.PRODUCTIVITY, TaskDomain.PRACTICAL, allowed, Map.of(), roles, prepared.actions(),
                null, null, ActionIntent.PLAN,
                List.of(new FallbackCandidate(ActionIntent.PLAN, "Productivity default: plan schedules or tasks; override with GENERATE/ANALYZE as needed.")),
                prepared.groupMap()));
    }

    private void registerMarketing(Map<PromptCategory, CategorySemanticProfile> target) {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.GENERATE, List.of(MarketingRoleType.DIGITAL_MARKETER, MarketingRoleType.MARKETING_STRATEGIST));
        roles.put(ActionIntent.PLAN, List.of(MarketingRoleType.MARKETING_STRATEGIST, MarketingRoleType.BRAND_SPECIALIST));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.GENERATE, List.of(MarketingActionType.CONTENT_MARKETING, MarketingActionType.AD_CAMPAIGN));
        actions.put(ActionIntent.PLAN, List.of(MarketingActionType.MARKETING_STRATEGY, MarketingActionType.SOCIAL_MEDIA_STRATEGY));

        Set<ActionIntent> allowed = Set.of(ActionIntent.GENERATE, ActionIntent.PLAN, ActionIntent.ANALYZE, ActionIntent.REWRITE);
        GroupMapAndActions prepared = prepare(actions);
        target.put(PromptCategory.MARKETING, new DefaultCategorySemanticProfile(
                PromptCategory.MARKETING, TaskDomain.PRACTICAL, allowed, Map.of(), roles, prepared.actions(),
                null, null, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Marketing default: generate content or campaigns; use PLAN/ANALYZE/REWRITE as needed.")),
                prepared.groupMap()));
    }

    private void registerCustomerSupport(Map<PromptCategory, CategorySemanticProfile> target) {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.GENERATE, List.of(CustomerSupportRoleType.CUSTOMER_SUPPORT_SPECIALIST, CustomerSupportRoleType.TECHNICAL_SUPPORT_ENGINEER));
        roles.put(ActionIntent.EXPLAIN, List.of(CustomerSupportRoleType.TECHNICAL_SUPPORT_ENGINEER, CustomerSupportRoleType.CUSTOMER_SUCCESS_MANAGER));
        roles.put(ActionIntent.PLAN, List.of(CustomerSupportRoleType.CUSTOMER_SUCCESS_MANAGER, CustomerSupportRoleType.SUPPORT_TRAINER));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.GENERATE, List.of(CustomerSupportActionType.FAQ_CREATION, CustomerSupportActionType.SUPPORT_DOCUMENTATION));
        actions.put(ActionIntent.EXPLAIN, List.of(CustomerSupportActionType.SUPPORT_DOCUMENTATION, CustomerSupportActionType.KNOWLEDGE_BASE_MANAGEMENT));
        actions.put(ActionIntent.PLAN, List.of(CustomerSupportActionType.CUSTOMER_ONBOARDING, CustomerSupportActionType.SUPPORT_TRAINING));

        Set<ActionIntent> allowed = Set.of(ActionIntent.GENERATE, ActionIntent.EXPLAIN, ActionIntent.PLAN, ActionIntent.SUMMARIZE, ActionIntent.ANALYZE);
        GroupMapAndActions prepared = prepare(actions);
        target.put(PromptCategory.CUSTOMER_SUPPORT, new DefaultCategorySemanticProfile(
                PromptCategory.CUSTOMER_SUPPORT, TaskDomain.PRACTICAL, allowed, Map.of(), roles, prepared.actions(),
                null, null, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Customer support default: generate FAQs or docs; use EXPLAIN/PLAN as needed.")),
                prepared.groupMap()));
    }

    private void registerDataAnalysis(Map<PromptCategory, CategorySemanticProfile> target) {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.ANALYZE, List.of(EtcRoleType.GENERAL_CONSULTANT));
        roles.put(ActionIntent.EVALUATE, List.of(EtcRoleType.GENERAL_CONSULTANT));
        roles.put(ActionIntent.EXTRACT, List.of(EtcRoleType.GENERAL_CONSULTANT));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.ANALYZE, List.of(AnalysisActionType.DATA_ANALYSIS, AnalysisActionType.COMPARATIVE_ANALYSIS, AnalysisActionType.ROOT_CAUSE_ANALYSIS));
        actions.put(ActionIntent.EVALUATE, List.of(AnalysisActionType.COMPARATIVE_ANALYSIS));
        actions.put(ActionIntent.EXTRACT, List.of(AnalysisActionType.DATA_ANALYSIS));

        Set<ActionIntent> allowed = Set.of(ActionIntent.ANALYZE, ActionIntent.EVALUATE, ActionIntent.EXTRACT, ActionIntent.SUMMARIZE, ActionIntent.CLASSIFY);
        GroupMapAndActions prepared = prepare(actions);
        target.put(PromptCategory.DATA_ANALYSIS, new DefaultCategorySemanticProfile(
                PromptCategory.DATA_ANALYSIS, TaskDomain.ANALYTICAL, allowed, Map.of(), roles, prepared.actions(),
                null, null, ActionIntent.ANALYZE,
                List.of(new FallbackCandidate(ActionIntent.ANALYZE, "Data analysis default: analyze or evaluate data; use EXTRACT/CLASSIFY for structured output.")),
                prepared.groupMap()));
    }

    private void registerLegal(Map<PromptCategory, CategorySemanticProfile> target) {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.EVALUATE, List.of(BusinessRoleType.BUSINESS_ANALYST_BUSINESS));
        roles.put(ActionIntent.GENERATE, List.of(BusinessRoleType.BUSINESS_CONSULTANT));
        roles.put(ActionIntent.EXPLAIN, List.of(BusinessRoleType.BUSINESS_CONSULTANT));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.EVALUATE, List.of(BusinessActionType.CONTRACT_REVIEW, BusinessActionType.RISK_ASSESSMENT));
        actions.put(ActionIntent.GENERATE, List.of(BusinessActionType.CONTRACT_REVIEW, BusinessActionType.PROPOSAL_WRITING));
        actions.put(ActionIntent.EXPLAIN, List.of(BusinessActionType.CONTRACT_REVIEW));

        Set<ActionIntent> allowed = Set.of(ActionIntent.EVALUATE, ActionIntent.GENERATE, ActionIntent.EXPLAIN, ActionIntent.SUMMARIZE);
        GroupMapAndActions prepared = prepare(actions);
        target.put(PromptCategory.LEGAL, new DefaultCategorySemanticProfile(
                PromptCategory.LEGAL, TaskDomain.PRACTICAL, allowed, Map.of(), roles, prepared.actions(),
                null, null, ActionIntent.EVALUATE,
                List.of(new FallbackCandidate(ActionIntent.EVALUATE, "Legal default: evaluate contracts or risks; use GENERATE/EXPLAIN as needed.")),
                prepared.groupMap()));
    }

    private void registerCreative(Map<PromptCategory, CategorySemanticProfile> target) {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.GENERATE, List.of(CreativeRoleType.CREATIVE_DIRECTOR, CreativeRoleType.STORYTELLER));
        roles.put(ActionIntent.REWRITE, List.of(CreativeRoleType.CREATIVE_DIRECTOR, CreativeRoleType.STORYTELLER));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.GENERATE, List.of(CreativeActionType.IDEA_GENERATION, CreativeActionType.CREATIVE_WRITING));
        actions.put(ActionIntent.REWRITE, List.of(CreativeActionType.CREATIVE_WRITING));

        Set<ActionIntent> allowed = Set.of(ActionIntent.GENERATE, ActionIntent.REWRITE, ActionIntent.EXPLAIN, ActionIntent.PLAN);
        GroupMapAndActions prepared = prepare(actions);
        target.put(PromptCategory.CREATIVE, new DefaultCategorySemanticProfile(
                PromptCategory.CREATIVE, TaskDomain.CREATIVE, allowed, Map.of(), roles, prepared.actions(),
                null, null, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Creative default: generate or rewrite; use EXPLAIN/PLAN as needed.")),
                prepared.groupMap()));
    }

    private void registerEducation(Map<PromptCategory, CategorySemanticProfile> target) {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.EXPLAIN, List.of(EducationRoleType.CURRICULUM_DESIGNER, EducationRoleType.EDUCATOR));
        roles.put(ActionIntent.PLAN, List.of(EducationRoleType.CURRICULUM_DESIGNER, EducationRoleType.INSTRUCTIONAL_DESIGNER));
        roles.put(ActionIntent.GENERATE, List.of(EducationRoleType.EDUCATOR, EducationRoleType.INSTRUCTIONAL_DESIGNER));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.EXPLAIN, List.of(EducationActionType.TEACHING_METHOD, EducationActionType.LESSON_PLANNING));
        actions.put(ActionIntent.PLAN, List.of(EducationActionType.LESSON_PLANNING, EducationActionType.CURRICULUM_DESIGN));
        actions.put(ActionIntent.GENERATE, List.of(EducationActionType.ASSESSMENT_DESIGN, EducationActionType.MATERIAL_CREATION));

        Set<ActionIntent> allowed = Set.of(ActionIntent.EXPLAIN, ActionIntent.PLAN, ActionIntent.GENERATE, ActionIntent.SUMMARIZE);
        GroupMapAndActions prepared = prepare(actions);
        target.put(PromptCategory.EDUCATION, new DefaultCategorySemanticProfile(
                PromptCategory.EDUCATION, TaskDomain.EDUCATIONAL, allowed, Map.of(), roles, prepared.actions(),
                null, null, ActionIntent.EXPLAIN,
                List.of(new FallbackCandidate(ActionIntent.EXPLAIN, "Education default: explain or plan; use GENERATE/SUMMARIZE as needed.")),
                prepared.groupMap()));
    }

    private void registerEtc(Map<PromptCategory, CategorySemanticProfile> target) {
        Set<ActionIntent> allowed = Set.of(ActionIntent.GENERATE, ActionIntent.EXPLAIN, ActionIntent.PLAN, ActionIntent.ANALYZE, ActionIntent.REWRITE, ActionIntent.SUMMARIZE,
                ActionIntent.EVALUATE, ActionIntent.EXTRACT, ActionIntent.CLASSIFY, ActionIntent.DECIDE, ActionIntent.DIAGNOSE, ActionIntent.CREATE,
                ActionIntent.EDIT, ActionIntent.REFINE, ActionIntent.IMPROVE, ActionIntent.COMPARE, ActionIntent.CRITIQUE, ActionIntent.TEACH, ActionIntent.SIMPLIFY,
                ActionIntent.OUTLINE, ActionIntent.STRATEGIZE, ActionIntent.PROPOSE, ActionIntent.ORGANIZE, ActionIntent.RECOMMEND, ActionIntent.OPTIMIZE,
                ActionIntent.INVESTIGATE, ActionIntent.SYNTHESIZE, ActionIntent.EXPLORE, ActionIntent.BRAINSTORM);
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        GroupMapAndActions prepared = prepare(actions);
        target.put(PromptCategory.ETC, new DefaultCategorySemanticProfile(
                PromptCategory.ETC, TaskDomain.GENERAL, allowed, Map.of(), roles, prepared.actions(),
                null, null, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Etc: broad intents allowed; GENERATE is default when unspecified.")),
                prepared.groupMap()));
    }
}
