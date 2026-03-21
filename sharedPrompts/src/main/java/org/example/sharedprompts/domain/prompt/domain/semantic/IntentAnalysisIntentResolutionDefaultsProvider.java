package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.output.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;

import java.util.List;

final class IntentAnalysisIntentResolutionDefaultsProvider implements IntentResolutionDefaultsEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentResolutionDefaultEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.ANALYZE,
                        new IntentResolutionDefaults(PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.EVALUATE,
                        new IntentResolutionDefaults(PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.COMPARE,
                        new IntentResolutionDefaults(PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.CRITIQUE,
                        new IntentResolutionDefaults(PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.DIAGNOSE,
                        new IntentResolutionDefaults(PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STEP_BY_STEP)
                )
        );
    }
}

