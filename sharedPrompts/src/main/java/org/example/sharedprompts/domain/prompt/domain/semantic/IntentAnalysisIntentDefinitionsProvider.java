package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.List;

final class IntentAnalysisIntentDefinitionsProvider implements IntentDefinitionEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentDefinitionEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.ANALYZE,
                        new IntentDefinition(
                                ActionIntent.ANALYZE,
                                "Break down a subject into components, patterns, or factors; systematic examination.",
                                "User wants understanding of parts, causes, or patterns in data, text, or a system.",
                                "When the goal is judgment of merit (use EVALUATE), comparison (use COMPARE), or fault-finding (use DIAGNOSE).",
                                "ANALYZE = break down and examine; EVALUATE = judge merit; CRITIQUE = judge with standards; DIAGNOSE = find cause of problem.",
                                "Structured analysis; components, factors, or patterns.",
                                List.of(PromptCategory.ANALYSIS, PromptCategory.DATA_ANALYSIS, PromptCategory.RESEARCH, PromptCategory.BUSINESS),
                                List.of("DATA_ANALYSIS", "LITERATURE_REVIEW", "ROOT_CAUSE_ANALYSIS"),
                                List.of("GENERAL_CONSULTANT", "RESEARCHER", "BUSINESS_ANALYST_BUSINESS"),
                                "Core intent for ANALYSIS and RESEARCH categories."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.EVALUATE,
                        new IntentDefinition(
                                ActionIntent.EVALUATE,
                                "Judge merit, quality, or suitability against criteria or goals.",
                                "User wants an assessment of how good, suitable, or effective something is.",
                                "When the goal is only to break down or describe (use ANALYZE) or to compare two things (use COMPARE).",
                                "EVALUATE = judge merit; ANALYZE = examine components; CRITIQUE = evaluate against explicit standards; COMPARE = compare alternatives.",
                                "Structured evaluation with criteria and judgment.",
                                List.of(PromptCategory.DESIGN, PromptCategory.DEVELOPMENT, PromptCategory.RESEARCH),
                                List.of("CODE_REVIEW", "UX_DESIGN", "COMPARATIVE_ANALYSIS"),
                                List.of("UI_UX_DESIGNER", "PRODUCT_DESIGNER", "RESEARCHER"),
                                "DESIGN: preferred for design review and critique flows."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.COMPARE,
                        new IntentDefinition(
                                ActionIntent.COMPARE,
                                "Place two or more items side by side; highlight similarities and differences.",
                                "User wants a direct comparison of options, versions, or alternatives.",
                                "When the goal is single-item analysis (ANALYZE) or a merit judgment (EVALUATE).",
                                "COMPARE = side-by-side comparison; ANALYZE = single subject breakdown; EVALUATE = merit judgment.",
                                "Structured comparison; pros/cons or dimension-based.",
                                List.of(PromptCategory.ANALYSIS, PromptCategory.DATA_ANALYSIS, PromptCategory.BUSINESS, PromptCategory.RESEARCH),
                                List.of("COMPARATIVE_ANALYSIS", "DATA_ANALYSIS"),
                                List.of("BUSINESS_ANALYST_BUSINESS", "RESEARCHER"),
                                "Fits ANALYSIS and RESEARCH; often used with DECIDE or RECOMMEND."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.CRITIQUE,
                        new IntentDefinition(
                                ActionIntent.CRITIQUE,
                                "Judge against explicit standards or criteria; constructive criticism.",
                                "User wants feedback that evaluates against defined standards (e.g. design principles, style guide).",
                                "When the goal is neutral analysis (ANALYZE) or open-ended evaluation without standards (EVALUATE).",
                                "CRITIQUE = judge against standards; EVALUATE = general merit; ANALYZE = break down without judgment.",
                                "Structured critique with criteria and suggestions.",
                                List.of(PromptCategory.DESIGN, PromptCategory.WRITING, PromptCategory.RESEARCH),
                                List.of("UI_DESIGN", "UX_DESIGN", "EDITING"),
                                List.of("UI_UX_DESIGNER", "PRODUCT_DESIGNER", "EDITOR"),
                                "DESIGN: preferred for design review; DIAGNOSE only in narrow debug/critique contexts."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.DIAGNOSE,
                        new IntentDefinition(
                                ActionIntent.DIAGNOSE,
                                "Identify cause of a problem or failure; root-cause or fault-finding.",
                                "User has a problem (bug, failure, issue) and wants to understand why it occurs.",
                                "When the goal is general analysis (ANALYZE), merit evaluation (EVALUATE), or design critique (CRITIQUE).",
                                "DIAGNOSE = find cause of problem; ANALYZE = general breakdown; CRITIQUE = judge against standards.",
                                "Step-by-step or structured diagnosis; cause-and-effect.",
                                List.of(PromptCategory.DEVELOPMENT, PromptCategory.ANALYSIS, PromptCategory.DATA_ANALYSIS),
                                List.of("DEBUGGING", "CODE_ANALYSIS", "ROOT_CAUSE_ANALYSIS"),
                                List.of("BACKEND_DEVELOPER", "FRONTEND_DEVELOPER"),
                                "DESIGN: discouraged unless critique/debug context is explicit (e.g. usability bug)."
                        )
                )
        );
    }
}

