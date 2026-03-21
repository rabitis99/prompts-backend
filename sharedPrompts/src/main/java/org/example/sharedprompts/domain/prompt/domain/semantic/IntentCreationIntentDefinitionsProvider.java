package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.List;

final class IntentCreationIntentDefinitionsProvider implements IntentDefinitionEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentDefinitionEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.CREATE,
                        new IntentDefinition(
                                ActionIntent.CREATE,
                                "Author new artifact from scratch; emphasis on original composition and ownership.",
                                "User wants to author something new (document, design, piece of content) where originality and structure matter.",
                                "When the goal is to produce output quickly from a spec (use GENERATE) or to list ideas without a single artifact (use BRAINSTORM).",
                                "CREATE = original authored composition; GENERATE = produce requested output (may be template-driven); BRAINSTORM = ideation, multiple options.",
                                "Narrative or structured new artifact; single coherent output.",
                                List.of(PromptCategory.DESIGN, PromptCategory.WRITING, PromptCategory.CONTENT_CREATION, PromptCategory.CREATIVE),
                                List.of("UI_DESIGN", "UX_DESIGNER", "ARTICLE_WRITING", "CONTENT_CREATION"),
                                List.of("UI_UX_DESIGNER", "CONTENT_WRITER", "CREATIVE_DIRECTOR"),
                                "In CONTENT: prefer CREATE for long-form authored pieces; GENERATE for quick posts or templated output."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.GENERATE,
                        new IntentDefinition(
                                ActionIntent.GENERATE,
                                "Produce requested output from a description or spec; emphasis on fulfilling the request efficiently.",
                                "User wants the system to produce something (code, copy, design, list) that matches a given description or template.",
                                "When the user explicitly wants original authorship or heavy creative ownership (use CREATE).",
                                "GENERATE = produce to spec; CREATE = author from scratch with stronger creative control.",
                                "Narrative or structured output matching the request; may be more template- or spec-driven than CREATE.",
                                List.of(PromptCategory.DEVELOPMENT, PromptCategory.CONTENT_CREATION, PromptCategory.MARKETING, PromptCategory.CREATIVE),
                                List.of("CODE_GENERATION", "CONTENT_CREATION", "CONTENT_MARKETING"),
                                List.of("FULL_STACK_DEVELOPER", "CONTENT_CREATOR", "DIGITAL_MARKETER"),
                                "Default fallback for many categories when intent is unspecified."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.BRAINSTORM,
                        new IntentDefinition(
                                ActionIntent.BRAINSTORM,
                                "Generate multiple ideas, options, or directions without committing to a single artifact.",
                                "User wants ideation, alternatives, or exploratory options rather than one final deliverable.",
                                "When the user wants a single concrete output (use CREATE or GENERATE).",
                                "BRAINSTORM = many ideas; CREATE/GENERATE = one deliverable.",
                                "Structured list or short descriptions of options; no single narrative.",
                                List.of(PromptCategory.CREATIVE, PromptCategory.BUSINESS, PromptCategory.DESIGN),
                                List.of("IDEA_GENERATION", "CREATIVE_WRITING"),
                                List.of("CREATIVE_DIRECTOR", "MARKETING_STRATEGIST"),
                                "Often pairs with CREATIVE or BUSINESS; less common in DEVELOPMENT for code."
                        )
                )
        );
    }
}

