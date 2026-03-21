package org.example.sharedprompts.domain.prompt.domain.semantic.seed;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.research.ResearchActionType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.research.ResearchRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeed;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeedDefinition;
import org.example.sharedprompts.domain.prompt.domain.semantic.FallbackCandidate;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticFitLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResearchSemanticProfileSeed {

    public static final CategorySemanticProfileSeedDefinition DEFINITION =
            new CategorySemanticProfileSeedDefinition(PromptCategory.RESEARCH, ResearchSemanticProfileSeed::build);

    private ResearchSemanticProfileSeed() {
    }

    private static CategorySemanticProfileSeed build() {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.INVESTIGATE, List.of(ResearchRoleType.RESEARCHER, ResearchRoleType.RESEARCH_METHODOLOGIST));
        roles.put(ActionIntent.ANALYZE, List.of(ResearchRoleType.RESEARCHER, ResearchRoleType.RESEARCH_METHODOLOGIST));
        roles.put(ActionIntent.SYNTHESIZE, List.of(ResearchRoleType.RESEARCHER, ResearchRoleType.ACADEMIC_WRITER));
        roles.put(ActionIntent.EXPLAIN, List.of(ResearchRoleType.RESEARCH_METHODOLOGIST, ResearchRoleType.RESEARCHER));
        roles.put(ActionIntent.SUMMARIZE, List.of(ResearchRoleType.RESEARCHER, ResearchRoleType.ACADEMIC_WRITER));
        roles.put(ActionIntent.GENERATE, List.of(ResearchRoleType.ACADEMIC_WRITER, ResearchRoleType.RESEARCHER));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.INVESTIGATE, List.of(ResearchActionType.DATA_INTERPRETATION, ResearchActionType.LITERATURE_REVIEW));
        actions.put(ActionIntent.ANALYZE, List.of(ResearchActionType.DATA_INTERPRETATION, ResearchActionType.LITERATURE_REVIEW, ResearchActionType.STATISTICAL_MODELING));
        actions.put(ActionIntent.SYNTHESIZE, List.of(ResearchActionType.LITERATURE_REVIEW, ResearchActionType.STATISTICAL_MODELING));
        actions.put(ActionIntent.SUMMARIZE, List.of(ResearchActionType.LITERATURE_REVIEW, ResearchActionType.PAPER_WRITING));
        actions.put(ActionIntent.EXPLAIN, List.of(ResearchActionType.RESEARCH_DESIGN, ResearchActionType.METHODOLOGY_DEVELOPMENT));
        actions.put(ActionIntent.GENERATE, List.of(ResearchActionType.PAPER_WRITING, ResearchActionType.HYPOTHESIS_FORMULATION));

        Set<ActionIntent> allowed = Set.of(ActionIntent.INVESTIGATE, ActionIntent.ANALYZE, ActionIntent.SYNTHESIZE, ActionIntent.EXPLAIN, ActionIntent.SUMMARIZE, ActionIntent.GENERATE, ActionIntent.EVALUATE, ActionIntent.EXTRACT);
        Map<ActionIntent, SemanticFitLevel> fitLevels = new HashMap<>();
        fitLevels.put(ActionIntent.INVESTIGATE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.ANALYZE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.SYNTHESIZE, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.EXPLAIN, SemanticFitLevel.PREFERRED);
        fitLevels.put(ActionIntent.SUMMARIZE, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.GENERATE, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.EVALUATE, SemanticFitLevel.ALLOWED);
        fitLevels.put(ActionIntent.EXTRACT, SemanticFitLevel.ALLOWED);

        Map<ActionIntent, List<ToneType>> discouragedTones = new HashMap<>();
        discouragedTones.put(ActionIntent.ANALYZE, List.of(ToneType.MOTIVATIONAL, ToneType.ENTHUSIASTIC));
        discouragedTones.put(ActionIntent.SYNTHESIZE, List.of(ToneType.MOTIVATIONAL, ToneType.ENTHUSIASTIC));
        discouragedTones.put(ActionIntent.INVESTIGATE, List.of(ToneType.MOTIVATIONAL));

        return new CategorySemanticProfileSeed(
                PromptCategory.RESEARCH, TaskDomain.ANALYTICAL, allowed, fitLevels, roles, actions,
                discouragedTones, null, ActionIntent.ANALYZE,
                List.of(new FallbackCandidate(ActionIntent.ANALYZE, "Research default: analyze or investigate; use SYNTHESIZE for literature/evidence synthesis.")));
    }
}
