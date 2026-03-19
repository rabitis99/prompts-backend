package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.output.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;

import java.util.List;

final class IntentExplanationIntentResolutionDefaultsProvider implements IntentResolutionDefaultsEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentResolutionDefaultEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.EXPLAIN,
                        new IntentResolutionDefaults(PromptObjective.REASONING, OutputNeeds.FREE_FORM, ResponseShape.STEP_BY_STEP)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.TEACH,
                        new IntentResolutionDefaults(PromptObjective.REASONING, OutputNeeds.FREE_FORM, ResponseShape.STEP_BY_STEP)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.SIMPLIFY,
                        new IntentResolutionDefaults(PromptObjective.REASONING, OutputNeeds.FREE_FORM, ResponseShape.STRUCTURED)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.SUMMARIZE,
                        new IntentResolutionDefaults(PromptObjective.FACTUAL, OutputNeeds.BULLET_LIST_REQUIRED, ResponseShape.CONCISE)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.OUTLINE,
                        new IntentResolutionDefaults(PromptObjective.PLANNING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED)
                )
        );
    }
}

