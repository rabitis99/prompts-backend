package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.List;

final class IntentModificationIntentDefinitionsProvider implements IntentDefinitionEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentDefinitionEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.REWRITE,
                        new IntentDefinition(
                                ActionIntent.REWRITE,
                                "Substantially restructure or re-express existing content; different form or voice.",
                                "User wants the same message or content in a new structure, tone, or format (e.g. formal→casual, long→short).",
                                "When only local corrections or light edits are needed (use EDIT) or polish without restructuring (use REFINE).",
                                "REWRITE = substantial restructuring/re-expression; EDIT = correctness and local fixes; REFINE = polish; IMPROVE = broader clarity/effectiveness.",
                                "Structured or narrative output in the new form; length may change.",
                                List.of(PromptCategory.WRITING, PromptCategory.CONTENT_CREATION, PromptCategory.CREATIVE),
                                List.of("EDITING", "CONTENT_REVISION", "CREATIVE_WRITING"),
                                List.of("EDITOR", "COPYWRITER", "CONTENT_STRATEGIST"),
                                "WRITING category: REWRITE and EDIT must be clearly distinguished in profiles."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.EDIT,
                        new IntentDefinition(
                                ActionIntent.EDIT,
                                "Fix correctness, grammar, or local issues; preserve structure and voice.",
                                "User wants corrections, fixes, or small changes without changing the overall structure or tone.",
                                "When substantial restructuring or re-expression is needed (use REWRITE) or quality polish (use REFINE).",
                                "EDIT = correctness/local fixes; REWRITE = re-expression; REFINE = polish; IMPROVE = broader enhancement.",
                                "Same structure as input with corrections; minimal structural change.",
                                List.of(PromptCategory.WRITING, PromptCategory.CONTENT_CREATION),
                                List.of("EDITING", "PROOFREADING", "DOC_UPDATE"),
                                List.of("EDITOR", "CONTENT_WRITER"),
                                "Preferred for copy-editing and proofreading flows."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.REFINE,
                        new IntentDefinition(
                                ActionIntent.REFINE,
                                "Polish and elevate quality of existing content; improve clarity and style without major restructuring.",
                                "User wants to improve flow, word choice, and quality while keeping the same message and structure.",
                                "When structural changes are needed (use REWRITE) or only typo/grammar fixes (use EDIT).",
                                "REFINE = polish and quality; EDIT = correctness; REWRITE = restructure; IMPROVE = broader effectiveness.",
                                "Same structure, higher quality; more polished language.",
                                List.of(PromptCategory.WRITING, PromptCategory.DEVELOPMENT),
                                List.of("EDITING", "CODE_MODIFICATION"),
                                List.of("EDITOR", "COPYWRITER"),
                                "DEVELOPMENT: REFINE for code quality and readability improvements."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.IMPROVE,
                        new IntentDefinition(
                                ActionIntent.IMPROVE,
                                "Broader enhancement of clarity, effectiveness, or impact; may include structure and content.",
                                "User wants overall improvement (readability, impact, clarity) without specifying exact type of change.",
                                "When the need is narrowly correctness (EDIT), polish only (REFINE), or full re-expression (REWRITE).",
                                "IMPROVE = broad enhancement; REFINE = polish; EDIT = fixes; REWRITE = re-express.",
                                "Improved version; may include structural tweaks and content adjustments.",
                                List.of(PromptCategory.WRITING, PromptCategory.DEVELOPMENT, PromptCategory.CONTENT_CREATION),
                                List.of("EDITING", "CODE_MODIFICATION", "CONTENT_OPTIMIZATION"),
                                List.of("EDITOR", "CONTENT_WRITER", "BACKEND_DEVELOPER"),
                                "General-purpose improvement intent across writing and development."
                        )
                )
        );
    }
}

