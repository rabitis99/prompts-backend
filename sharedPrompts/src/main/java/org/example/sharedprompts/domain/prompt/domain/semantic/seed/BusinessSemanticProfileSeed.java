package org.example.sharedprompts.domain.prompt.domain.semantic.seed;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.business.BusinessActionType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.business.BusinessRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeed;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeedDefinition;
import org.example.sharedprompts.domain.prompt.domain.semantic.FallbackCandidate;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticFitLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BusinessSemanticProfileSeed {

    public static final CategorySemanticProfileSeedDefinition DEFINITION =
            new CategorySemanticProfileSeedDefinition(PromptCategory.BUSINESS, BusinessSemanticProfileSeed::build);

    private BusinessSemanticProfileSeed() {
    }

    private static CategorySemanticProfileSeed build() {
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

        return new CategorySemanticProfileSeed(
                PromptCategory.BUSINESS, TaskDomain.PRACTICAL, allowed, fitLevels, roles, actions,
                null, null, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Business default: generate proposals/reports; use PLAN/PROPOSE/DECIDE for strategy and decisions.")));
    }
}
