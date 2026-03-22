package org.example.sharedprompts.domain.prompt.domain.semantic.seed;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.analysis.AnalysisActionType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.etc.EtcRoleType;
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

public final class DataAnalysisSemanticProfileSeed {

    public static final CategorySemanticProfileSeedDefinition DEFINITION =
            new CategorySemanticProfileSeedDefinition(PromptCategory.DATA_ANALYSIS, DataAnalysisSemanticProfileSeed::build);

    private DataAnalysisSemanticProfileSeed() {
    }

    private static CategorySemanticProfileSeed build() {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.ANALYZE, List.of(EtcRoleType.GENERAL_CONSULTANT));
        roles.put(ActionIntent.EVALUATE, List.of(EtcRoleType.GENERAL_CONSULTANT));
        roles.put(ActionIntent.EXTRACT, List.of(EtcRoleType.GENERAL_CONSULTANT));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.ANALYZE, List.of(AnalysisActionType.DATA_ANALYSIS, AnalysisActionType.COMPARATIVE_ANALYSIS, AnalysisActionType.ROOT_CAUSE_ANALYSIS));
        actions.put(ActionIntent.EVALUATE, List.of(AnalysisActionType.COMPARATIVE_ANALYSIS));
        actions.put(ActionIntent.EXTRACT, List.of(AnalysisActionType.DATA_ANALYSIS));

        Set<ActionIntent> allowed = Set.of(ActionIntent.ANALYZE, ActionIntent.EVALUATE, ActionIntent.EXTRACT, ActionIntent.SUMMARIZE, ActionIntent.CLASSIFY);
        return new CategorySemanticProfileSeed(
                PromptCategory.DATA_ANALYSIS, TaskDomain.ANALYTICAL, allowed, Map.of(), roles, actions,
                null, null, ActionIntent.ANALYZE,
                List.of(new FallbackCandidate(ActionIntent.ANALYZE, "Data analysis default: analyze or evaluate data; use EXTRACT/CLASSIFY for structured output.")));
    }
}
