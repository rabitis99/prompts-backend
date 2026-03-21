package org.example.sharedprompts.domain.prompt.domain.semantic.seed;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity.ProductivityActionType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.productivity.ProductivityRoleType;
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

public final class ProductivitySemanticProfileSeed {

    public static final CategorySemanticProfileSeedDefinition DEFINITION =
            new CategorySemanticProfileSeedDefinition(PromptCategory.PRODUCTIVITY, ProductivitySemanticProfileSeed::build);

    private ProductivitySemanticProfileSeed() {
    }

    private static CategorySemanticProfileSeed build() {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.PLAN, List.of(ProductivityRoleType.PRODUCTIVITY_EXPERT, ProductivityRoleType.TIME_MANAGEMENT_SPECIALIST));
        roles.put(ActionIntent.GENERATE, List.of(ProductivityRoleType.PRODUCTIVITY_EXPERT));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.PLAN, List.of(ProductivityActionType.SCHEDULE_PLANNING, ProductivityActionType.TASK_AUTOMATION));

        Set<ActionIntent> allowed = Set.of(ActionIntent.PLAN, ActionIntent.GENERATE, ActionIntent.ANALYZE);
        return new CategorySemanticProfileSeed(
                PromptCategory.PRODUCTIVITY, TaskDomain.PRACTICAL, allowed, Map.of(), roles, actions,
                null, null, ActionIntent.PLAN,
                List.of(new FallbackCandidate(ActionIntent.PLAN, "Productivity default: plan schedules or tasks; override with GENERATE/ANALYZE as needed.")));
    }
}
