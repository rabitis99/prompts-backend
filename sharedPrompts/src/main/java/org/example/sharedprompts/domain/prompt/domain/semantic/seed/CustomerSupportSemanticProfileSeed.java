package org.example.sharedprompts.domain.prompt.domain.semantic.seed;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.business.CustomerSupportActionType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.business.CustomerSupportRoleType;
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

public final class CustomerSupportSemanticProfileSeed {

    public static final CategorySemanticProfileSeedDefinition DEFINITION =
            new CategorySemanticProfileSeedDefinition(PromptCategory.CUSTOMER_SUPPORT, CustomerSupportSemanticProfileSeed::build);

    private CustomerSupportSemanticProfileSeed() {
    }

    private static CategorySemanticProfileSeed build() {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.GENERATE, List.of(CustomerSupportRoleType.CUSTOMER_SUPPORT_SPECIALIST, CustomerSupportRoleType.TECHNICAL_SUPPORT_ENGINEER));
        roles.put(ActionIntent.EXPLAIN, List.of(CustomerSupportRoleType.TECHNICAL_SUPPORT_ENGINEER, CustomerSupportRoleType.CUSTOMER_SUCCESS_MANAGER));
        roles.put(ActionIntent.PLAN, List.of(CustomerSupportRoleType.CUSTOMER_SUCCESS_MANAGER, CustomerSupportRoleType.SUPPORT_TRAINER));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.GENERATE, List.of(CustomerSupportActionType.FAQ_CREATION, CustomerSupportActionType.SUPPORT_DOCUMENTATION));
        actions.put(ActionIntent.EXPLAIN, List.of(CustomerSupportActionType.SUPPORT_DOCUMENTATION, CustomerSupportActionType.KNOWLEDGE_BASE_MANAGEMENT));
        actions.put(ActionIntent.PLAN, List.of(CustomerSupportActionType.CUSTOMER_ONBOARDING, CustomerSupportActionType.SUPPORT_TRAINING));

        Set<ActionIntent> allowed = Set.of(ActionIntent.GENERATE, ActionIntent.EXPLAIN, ActionIntent.PLAN, ActionIntent.SUMMARIZE, ActionIntent.ANALYZE);
        return new CategorySemanticProfileSeed(
                PromptCategory.CUSTOMER_SUPPORT, TaskDomain.PRACTICAL, allowed, Map.of(), roles, actions,
                null, null, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Customer support default: generate FAQs or docs; use EXPLAIN/PLAN as needed.")));
    }
}
