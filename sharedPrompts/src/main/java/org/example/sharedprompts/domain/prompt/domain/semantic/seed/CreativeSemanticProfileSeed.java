package org.example.sharedprompts.domain.prompt.domain.semantic.seed;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.creative.CreativeActionType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.creative.CreativeRoleType;
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

public final class CreativeSemanticProfileSeed {

    public static final CategorySemanticProfileSeedDefinition DEFINITION =
            new CategorySemanticProfileSeedDefinition(PromptCategory.CREATIVE, CreativeSemanticProfileSeed::build);

    private CreativeSemanticProfileSeed() {
    }

    private static CategorySemanticProfileSeed build() {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.GENERATE, List.of(CreativeRoleType.CREATIVE_DIRECTOR, CreativeRoleType.STORYTELLER));
        roles.put(ActionIntent.REWRITE, List.of(CreativeRoleType.CREATIVE_DIRECTOR, CreativeRoleType.STORYTELLER));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.GENERATE, List.of(CreativeActionType.IDEA_GENERATION, CreativeActionType.CREATIVE_WRITING));
        actions.put(ActionIntent.REWRITE, List.of(CreativeActionType.CREATIVE_WRITING));

        Set<ActionIntent> allowed = Set.of(ActionIntent.GENERATE, ActionIntent.REWRITE, ActionIntent.EXPLAIN, ActionIntent.PLAN);
        return new CategorySemanticProfileSeed(
                PromptCategory.CREATIVE, TaskDomain.CREATIVE, allowed, Map.of(), roles, actions,
                null, null, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Creative default: generate or rewrite; use EXPLAIN/PLAN as needed.")));
    }
}
