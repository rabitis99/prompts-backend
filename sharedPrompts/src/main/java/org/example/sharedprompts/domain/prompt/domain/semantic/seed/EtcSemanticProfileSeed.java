package org.example.sharedprompts.domain.prompt.domain.semantic.seed;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
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

public final class EtcSemanticProfileSeed {

    public static final CategorySemanticProfileSeedDefinition DEFINITION =
            new CategorySemanticProfileSeedDefinition(PromptCategory.ETC, EtcSemanticProfileSeed::build);

    private EtcSemanticProfileSeed() {
    }

    private static CategorySemanticProfileSeed build() {
        Set<ActionIntent> allowed = Set.of(ActionIntent.GENERATE, ActionIntent.EXPLAIN, ActionIntent.PLAN, ActionIntent.ANALYZE, ActionIntent.REWRITE, ActionIntent.SUMMARIZE,
                ActionIntent.EVALUATE, ActionIntent.EXTRACT, ActionIntent.CLASSIFY, ActionIntent.DECIDE, ActionIntent.DIAGNOSE, ActionIntent.CREATE,
                ActionIntent.EDIT, ActionIntent.REFINE, ActionIntent.IMPROVE, ActionIntent.COMPARE, ActionIntent.CRITIQUE, ActionIntent.TEACH, ActionIntent.SIMPLIFY,
                ActionIntent.OUTLINE, ActionIntent.STRATEGIZE, ActionIntent.PROPOSE, ActionIntent.ORGANIZE, ActionIntent.RECOMMEND, ActionIntent.OPTIMIZE,
                ActionIntent.INVESTIGATE, ActionIntent.SYNTHESIZE, ActionIntent.EXPLORE, ActionIntent.BRAINSTORM);
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        return new CategorySemanticProfileSeed(
                PromptCategory.ETC, TaskDomain.GENERAL, allowed, Map.of(), roles, actions,
                null, null, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Etc: broad intents allowed; GENERATE is default when unspecified.")));
    }
}
