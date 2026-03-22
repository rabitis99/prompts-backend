package org.example.sharedprompts.domain.prompt.domain.semantic.seed;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.writing.WritingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.writing.WritingRoleType;
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

public final class WritingSemanticProfileSeed {

    public static final CategorySemanticProfileSeedDefinition DEFINITION =
            new CategorySemanticProfileSeedDefinition(PromptCategory.WRITING, WritingSemanticProfileSeed::build);

    private WritingSemanticProfileSeed() {
    }

    private static CategorySemanticProfileSeed build() {
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

        return new CategorySemanticProfileSeed(
                PromptCategory.WRITING, TaskDomain.CREATIVE, allowed, fitLevels, roles, actions,
                null, null, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Writing default: generate new content; use REWRITE/EDIT/REFINE/IMPROVE for existing text.")));
    }
}
