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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LegalSemanticProfileSeed {

    public static final CategorySemanticProfileSeedDefinition DEFINITION =
            new CategorySemanticProfileSeedDefinition(PromptCategory.LEGAL, LegalSemanticProfileSeed::build);

    private LegalSemanticProfileSeed() {
    }

    private static CategorySemanticProfileSeed build() {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.EVALUATE, List.of(BusinessRoleType.BUSINESS_ANALYST_BUSINESS));
        roles.put(ActionIntent.GENERATE, List.of(BusinessRoleType.BUSINESS_CONSULTANT));
        roles.put(ActionIntent.EXPLAIN, List.of(BusinessRoleType.BUSINESS_CONSULTANT));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.EVALUATE, List.of(BusinessActionType.CONTRACT_REVIEW, BusinessActionType.RISK_ASSESSMENT));
        actions.put(ActionIntent.GENERATE, List.of(BusinessActionType.CONTRACT_REVIEW, BusinessActionType.PROPOSAL_WRITING));
        actions.put(ActionIntent.EXPLAIN, List.of(BusinessActionType.CONTRACT_REVIEW));

        Set<ActionIntent> allowed = Set.of(ActionIntent.EVALUATE, ActionIntent.GENERATE, ActionIntent.EXPLAIN, ActionIntent.SUMMARIZE);
        return new CategorySemanticProfileSeed(
                PromptCategory.LEGAL, TaskDomain.PRACTICAL, allowed, Map.of(), roles, actions,
                null, null, ActionIntent.EVALUATE,
                List.of(new FallbackCandidate(ActionIntent.EVALUATE, "Legal default: evaluate contracts or risks; use GENERATE/EXPLAIN as needed.")));
    }
}
