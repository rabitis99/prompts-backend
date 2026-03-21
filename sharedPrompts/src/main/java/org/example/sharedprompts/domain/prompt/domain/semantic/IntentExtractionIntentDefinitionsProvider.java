package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.List;

final class IntentExtractionIntentDefinitionsProvider implements IntentDefinitionEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentDefinitionEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.EXTRACT,
                        new IntentDefinition(
                                ActionIntent.EXTRACT,
                                "Pull structured data or entities from unstructured input; output in specified schema (e.g. JSON).",
                                "User wants to extract specific fields, entities, or facts from text or content.",
                                "When the goal is to classify into categories only (use CLASSIFY) or to summarize (use SUMMARIZE).",
                                "EXTRACT = structured extraction to schema; CLASSIFY = assign categories/labels.",
                                "Structured output (e.g. JSON); schema-driven.",
                                List.of(PromptCategory.EXTRACTION, PromptCategory.ANALYSIS, PromptCategory.DATA_ANALYSIS, PromptCategory.RESEARCH),
                                List.of("DATA_ANALYSIS"),
                                List.of("GENERAL_CONSULTANT"),
                                "EXTRACTION request_mode forces intent=EXTRACT; category=EXTRACTION."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.CLASSIFY,
                        new IntentDefinition(
                                ActionIntent.CLASSIFY,
                                "Assign category, label, or tag to input; classification or categorization.",
                                "User wants to classify content into predefined categories or labels.",
                                "When the goal is to extract multiple fields (use EXTRACT) or to summarize (use SUMMARIZE).",
                                "CLASSIFY = assign category; EXTRACT = extract fields to schema.",
                                "Structured labels or categories.",
                                List.of(PromptCategory.ANALYSIS, PromptCategory.DATA_ANALYSIS),
                                List.of("DATA_ANALYSIS"),
                                List.of("GENERAL_CONSULTANT"),
                                "Fits ANALYSIS and extraction-style flows."
                        )
                )
        );
    }
}

