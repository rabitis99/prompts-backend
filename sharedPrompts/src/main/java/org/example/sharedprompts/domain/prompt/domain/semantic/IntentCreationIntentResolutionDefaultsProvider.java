package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.output.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;

import java.util.List;

final class IntentCreationIntentResolutionDefaultsProvider implements IntentResolutionDefaultsEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentResolutionDefaultEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.CREATE,
                        new IntentResolutionDefaults(PromptObjective.CREATIVE, OutputNeeds.FREE_FORM, ResponseShape.NARRATIVE)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.GENERATE,
                        new IntentResolutionDefaults(PromptObjective.CREATIVE, OutputNeeds.FREE_FORM, ResponseShape.NARRATIVE)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.BRAINSTORM,
                        new IntentResolutionDefaults(PromptObjective.CREATIVE, OutputNeeds.FREE_FORM, ResponseShape.STRUCTURED)
                )
        );
    }
}

