package org.example.sharedprompts.domain.prompt.domain.semantic.seed;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.education.EducationActionType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.education.EducationRoleType;
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

public final class EducationSemanticProfileSeed {

    public static final CategorySemanticProfileSeedDefinition DEFINITION =
            new CategorySemanticProfileSeedDefinition(PromptCategory.EDUCATION, EducationSemanticProfileSeed::build);

    private EducationSemanticProfileSeed() {
    }

    private static CategorySemanticProfileSeed build() {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.EXPLAIN, List.of(EducationRoleType.CURRICULUM_DESIGNER, EducationRoleType.EDUCATOR));
        roles.put(ActionIntent.PLAN, List.of(EducationRoleType.CURRICULUM_DESIGNER, EducationRoleType.INSTRUCTIONAL_DESIGNER));
        roles.put(ActionIntent.GENERATE, List.of(EducationRoleType.EDUCATOR, EducationRoleType.INSTRUCTIONAL_DESIGNER));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.EXPLAIN, List.of(EducationActionType.TEACHING_METHOD, EducationActionType.LESSON_PLANNING));
        actions.put(ActionIntent.PLAN, List.of(EducationActionType.LESSON_PLANNING, EducationActionType.CURRICULUM_DESIGN));
        actions.put(ActionIntent.GENERATE, List.of(EducationActionType.ASSESSMENT_DESIGN, EducationActionType.MATERIAL_CREATION));

        Set<ActionIntent> allowed = Set.of(ActionIntent.EXPLAIN, ActionIntent.PLAN, ActionIntent.GENERATE, ActionIntent.SUMMARIZE);
        return new CategorySemanticProfileSeed(
                PromptCategory.EDUCATION, TaskDomain.EDUCATIONAL, allowed, Map.of(), roles, actions,
                null, null, ActionIntent.EXPLAIN,
                List.of(new FallbackCandidate(ActionIntent.EXPLAIN, "Education default: explain or plan; use GENERATE/SUMMARIZE as needed.")));
    }
}
