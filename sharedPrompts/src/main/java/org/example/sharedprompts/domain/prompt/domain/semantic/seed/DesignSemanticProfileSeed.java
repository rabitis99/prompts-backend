package org.example.sharedprompts.domain.prompt.domain.semantic.seed;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.design.DesignActionType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.design.DesignRoleType;
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

public final class DesignSemanticProfileSeed {

    public static final CategorySemanticProfileSeedDefinition DEFINITION =
            new CategorySemanticProfileSeedDefinition(PromptCategory.DESIGN, DesignSemanticProfileSeed::build);

    private DesignSemanticProfileSeed() {
    }

    private static CategorySemanticProfileSeed build() {
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
        fitLevels.put(ActionIntent.DIAGNOSE, SemanticFitLevel.DISCOURAGED);

        return new CategorySemanticProfileSeed(
                PromptCategory.DESIGN, TaskDomain.CREATIVE, allowed, fitLevels, roles, actions,
                null, null, ActionIntent.CREATE,
                List.of(new FallbackCandidate(ActionIntent.CREATE, "Design typically starts with creating or generating; CREATE is the default when intent is unspecified.")));
    }
}
