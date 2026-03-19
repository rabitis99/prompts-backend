package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.List;

final class IntentPlanningIntentDefinitionsProvider implements IntentDefinitionEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentDefinitionEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.PLAN,
                        new IntentDefinition(
                                ActionIntent.PLAN,
                                "Produce a sequence of steps or phases to achieve a goal.",
                                "User wants a plan: what to do, in what order, to reach an outcome.",
                                "When the goal is strategy (STRATEGIZE), a proposal (PROPOSE), or organization of existing items (ORGANIZE).",
                                "PLAN = steps/phases; STRATEGIZE = high-level approach; PROPOSE = suggest option; ORGANIZE = arrange existing.",
                                "Step-by-step or phased plan.",
                                List.of(PromptCategory.PRODUCTIVITY, PromptCategory.BUSINESS, PromptCategory.EDUCATION, PromptCategory.DEVELOPMENT),
                                List.of("SCHEDULE_PLANNING", "BUSINESS_PLAN_DEVELOPMENT", "LESSON_PLANNING", "ARCHITECTURE_DESIGN"),
                                List.of("PRODUCTIVITY_EXPERT", "PROJECT_MANAGER", "CURRICULUM_DESIGNER"),
                                "Fallback for PRODUCTIVITY when intent missing."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.STRATEGIZE,
                        new IntentDefinition(
                                ActionIntent.STRATEGIZE,
                                "Define high-level approach, direction, or strategy; not a detailed step list.",
                                "User wants strategic guidance: direction, principles, or approach rather than a task list.",
                                "When the goal is a concrete plan (PLAN) or a specific proposal (PROPOSE).",
                                "STRATEGIZE = high-level approach; PLAN = step sequence; PROPOSE = concrete suggestion.",
                                "Structured strategy; principles and priorities.",
                                List.of(PromptCategory.BUSINESS, PromptCategory.MARKETING),
                                List.of("BUSINESS_STRATEGY", "MARKETING_STRATEGY"),
                                List.of("BUSINESS_CONSULTANT", "MARKETING_STRATEGIST"),
                                "Often used with BUSINESS and MARKETING."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.PROPOSE,
                        new IntentDefinition(
                                ActionIntent.PROPOSE,
                                "Suggest a concrete option, solution, or recommendation for a decision.",
                                "User wants a specific proposal: what to do or choose, with rationale.",
                                "When the goal is open strategy (STRATEGIZE) or a neutral plan (PLAN).",
                                "PROPOSE = suggest option; RECOMMEND = recommend with reasoning; PLAN = steps.",
                                "Structured proposal with rationale.",
                                List.of(PromptCategory.BUSINESS, PromptCategory.DEVELOPMENT),
                                List.of("PROPOSAL_WRITING", "SYSTEM_DESIGN", "BUSINESS_PLAN_DEVELOPMENT"),
                                List.of("BUSINESS_CONSULTANT", "CLOUD_ARCHITECT"),
                                "BUSINESS: preferred for proposals and option presentation."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.ORGANIZE,
                        new IntentDefinition(
                                ActionIntent.ORGANIZE,
                                "Arrange existing items into structure, order, or categories.",
                                "User has existing items (ideas, tasks, content) and wants them ordered or grouped.",
                                "When the goal is to create new content (CREATE/GENERATE) or to plan from scratch (PLAN).",
                                "ORGANIZE = arrange existing; PLAN = define steps; OUTLINE = structure for content.",
                                "Structured list or taxonomy.",
                                List.of(PromptCategory.PRODUCTIVITY, PromptCategory.EDUCATION, PromptCategory.CONTENT_CREATION),
                                List.of("TASK_AUTOMATION", "KNOWLEDGE_ORGANIZATION", "CONTENT_PLANNING"),
                                List.of("PRODUCTIVITY_EXPERT", "STUDY_COACH"),
                                "Less common than PLAN; fits productivity and study."
                        )
                )
        );
    }
}

