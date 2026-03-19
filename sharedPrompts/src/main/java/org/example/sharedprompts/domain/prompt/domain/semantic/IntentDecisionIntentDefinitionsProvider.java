package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.List;

final class IntentDecisionIntentDefinitionsProvider implements IntentDefinitionEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentDefinitionEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.RECOMMEND,
                        new IntentDefinition(
                                ActionIntent.RECOMMEND,
                                "Recommend one or more options with reasoning; support decision-making.",
                                "User wants a recommendation: what to choose or do, with justification.",
                                "When the goal is only to compare (COMPARE) or to propose without ranking (PROPOSE).",
                                "RECOMMEND = recommend with reasoning; PROPOSE = suggest option; DECIDE = make the decision explicit.",
                                "Structured recommendation with rationale.",
                                List.of(PromptCategory.BUSINESS, PromptCategory.DESIGN, PromptCategory.DEVELOPMENT),
                                List.of("RISK_ASSESSMENT", "PRODUCT_DESIGN", "CODE_REVIEW"),
                                List.of("BUSINESS_CONSULTANT", "PRODUCT_DESIGNER"),
                                "DESIGN: allowed but weaker than CREATE/EVALUATE/CRITIQUE."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.OPTIMIZE,
                        new IntentDefinition(
                                ActionIntent.OPTIMIZE,
                                "Improve an existing solution against stated criteria (speed, cost, clarity).",
                                "User has something that works and wants it improved for specific dimensions.",
                                "When the goal is to fix broken (DIAGNOSE) or to evaluate only (EVALUATE).",
                                "OPTIMIZE = improve against criteria; IMPROVE = general enhancement; DIAGNOSE = find cause of failure.",
                                "Structured optimization suggestions; before/after or metrics.",
                                List.of(PromptCategory.DEVELOPMENT, PromptCategory.BUSINESS, PromptCategory.PRODUCTIVITY),
                                List.of("CODE_MODIFICATION", "BUSINESS_STRATEGY", "TASK_AUTOMATION"),
                                List.of("FULL_STACK_DEVELOPER", "BUSINESS_ANALYST_BUSINESS"),
                                "Fits technical and business optimization."
                        )
                ),
                new IntentDefinitionDataSource.IntentDefinitionEntry(
                        ActionIntent.DECIDE,
                        new IntentDefinition(
                                ActionIntent.DECIDE,
                                "Make or support a binary or multi-option decision; choose among alternatives.",
                                "User wants to decide between options or get support for a decision.",
                                "When the goal is only to recommend (RECOMMEND) or to analyze (ANALYZE).",
                                "DECIDE = choose; RECOMMEND = recommend with reasoning; PROPOSE = suggest option.",
                                "Structured decision with criteria and choice.",
                                List.of(PromptCategory.BUSINESS),
                                List.of("RISK_ASSESSMENT", "CONTRACT_REVIEW"),
                                List.of("BUSINESS_CONSULTANT", "BUSINESS_ANALYST_BUSINESS"),
                                "BUSINESS: preferred for decision-support flows."
                        )
                )
        );
    }
}

