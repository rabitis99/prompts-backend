package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.output.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;

import java.util.List;

final class IntentDecisionIntentResolutionDefaultsProvider implements IntentResolutionDefaultsEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentResolutionDefaultEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.RECOMMEND,
                        new IntentResolutionDefaults(PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.OPTIMIZE,
                        new IntentResolutionDefaults(PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.DECIDE,
                        new IntentResolutionDefaults(PromptObjective.REASONING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED)
                )
        );
    }
}

