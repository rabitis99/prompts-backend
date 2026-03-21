package org.example.sharedprompts.domain.prompt.domain.semantic.seed;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.CodingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.DevelopmentActionType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.development.DevelopmentRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeed;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeedDefinition;
import org.example.sharedprompts.domain.prompt.domain.semantic.FallbackCandidate;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticFitLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DevelopmentSemanticProfileSeed {

    public static final CategorySemanticProfileSeedDefinition DEFINITION =
            new CategorySemanticProfileSeedDefinition(PromptCategory.DEVELOPMENT, DevelopmentSemanticProfileSeed::build);

    private DevelopmentSemanticProfileSeed() {
    }

    private static CategorySemanticProfileSeed build() {
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

        return new CategorySemanticProfileSeed(
                PromptCategory.DEVELOPMENT, TaskDomain.TECHNICAL, allowed, fitLevels, roles, actions,
                null, discouragedStyles, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Development default: generate code or artifacts; override with intent for debug, explain, or plan.")));
    }
}
