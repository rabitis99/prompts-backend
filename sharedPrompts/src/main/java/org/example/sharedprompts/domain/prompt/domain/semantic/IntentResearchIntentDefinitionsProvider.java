package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.List;

final class IntentResearchIntentDefinitionsProvider implements IntentDefinitionEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentDefinitionEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.INVESTIGATE,
                        new IntentDefinition(
                                ActionIntent.INVESTIGATE,
                                "Systematically look into a question or topic; gather and examine evidence.",
                                "User wants to investigate a question: what is known, what are sources, what are findings.",
                                "When the goal is to synthesize into one view (SYNTHESIZE) or to explore freely (EXPLORE).",
                                "INVESTIGATE = systematic inquiry; SYNTHESIZE = combine into one view; EXPLORE = open exploration.",
                                "Structured findings; may include sources and caveats.",
                                List.of(PromptCategory.RESEARCH, PromptCategory.ANALYSIS, PromptCategory.DATA_ANALYSIS),
                                List.of("LITERATURE_REVIEW", "DATA_INTERPRETATION", "RESEARCH_DESIGN"),
                                List.of("RESEARCHER", "RESEARCH_METHODOLOGIST"),
                                "RESEARCH: preferred for literature and evidence-based inquiry."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.SYNTHESIZE,
                        new IntentDefinition(
                                ActionIntent.SYNTHESIZE,
                                "Combine multiple sources or views into a single coherent position or summary.",
                                "User has multiple inputs (papers, opinions, data) and wants them synthesized.",
                                "When the goal is to investigate (INVESTIGATE) or to explore (EXPLORE) without synthesis.",
                                "SYNTHESIZE = combine into one; INVESTIGATE = look into; EXPLORE = open exploration.",
                                "Structured synthesis; integrated view with sources.",
                                List.of(PromptCategory.RESEARCH, PromptCategory.ANALYSIS, PromptCategory.DATA_ANALYSIS),
                                List.of("LITERATURE_REVIEW", "STATISTICAL_MODELING", "DATA_INTERPRETATION"),
                                List.of("RESEARCHER", "ACADEMIC_WRITER"),
                                "RESEARCH: preferred for literature review and evidence synthesis; PERSUASIVE tone discouraged unless explicit."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.EXPLORE,
                        new IntentDefinition(
                                ActionIntent.EXPLORE,
                                "Open-ended exploration of ideas, options, or possibilities; less structured than investigate.",
                                "User wants to explore a space of ideas or options without a fixed question.",
                                "When the goal is systematic investigation (INVESTIGATE) or synthesis (SYNTHESIZE).",
                                "EXPLORE = open exploration; INVESTIGATE = systematic; SYNTHESIZE = combine into one.",
                                "Structured exploration; multiple angles or options.",
                                List.of(PromptCategory.RESEARCH, PromptCategory.CREATIVE),
                                List.of("IDEA_GENERATION", "LITERATURE_REVIEW"),
                                List.of("RESEARCHER", "CREATIVE_DIRECTOR"),
                                "Less formal than INVESTIGATE; fits creative and research."
                        )
                )
        );
    }
}

