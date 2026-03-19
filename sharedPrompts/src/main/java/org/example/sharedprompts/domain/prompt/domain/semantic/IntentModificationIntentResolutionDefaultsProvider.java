package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.output.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;

import java.util.List;

final class IntentModificationIntentResolutionDefaultsProvider implements IntentResolutionDefaultsEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentResolutionDefaultEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.REWRITE,
                        new IntentResolutionDefaults(PromptObjective.CREATIVE, OutputNeeds.FREE_FORM, ResponseShape.STRUCTURED)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.EDIT,
                        new IntentResolutionDefaults(PromptObjective.CREATIVE, OutputNeeds.FREE_FORM, ResponseShape.STRUCTURED)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.REFINE,
                        new IntentResolutionDefaults(PromptObjective.REASONING, OutputNeeds.FREE_FORM, ResponseShape.STRUCTURED)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.IMPROVE,
                        new IntentResolutionDefaults(PromptObjective.REASONING, OutputNeeds.FREE_FORM, ResponseShape.STRUCTURED)
                )
        );
    }
}

