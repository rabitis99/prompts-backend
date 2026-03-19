package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.output.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;

import java.util.List;

final class IntentPlanningIntentResolutionDefaultsProvider implements IntentResolutionDefaultsEntriesProvider {

    @Override
    public List<IntentDefinitionDataSource.IntentResolutionDefaultEntry> entries() {
        return List.of(
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.PLAN,
                        new IntentResolutionDefaults(PromptObjective.PLANNING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STEP_BY_STEP)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.STRATEGIZE,
                        new IntentResolutionDefaults(PromptObjective.PLANNING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.PROPOSE,
                        new IntentResolutionDefaults(PromptObjective.PLANNING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED)
                ),
                new IntentDefinitionDataSource.IntentResolutionDefaultEntry(
                        ActionIntent.ORGANIZE,
                        new IntentResolutionDefaults(PromptObjective.PLANNING, OutputNeeds.STRUCTURED_TEXT, ResponseShape.STRUCTURED)
                )
        );
    }
}

