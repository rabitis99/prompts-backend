package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDictionary;

import java.util.Objects;
import java.util.Optional;

/** legacy 라우팅용 Intent 기본값 */
public class IntentDefaultsResolver {

    private final IntentDictionary intentDictionary;

    public IntentDefaultsResolver(IntentDictionary intentDictionary) {
        this.intentDictionary = Objects.requireNonNull(intentDictionary, "intentDictionary");
    }

    public IntentDefaults resolve(UnifiedGeneratePromptCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ActionIntent intent = command.intent();
        if (intent == null) {
            intent = ActionIntent.GENERATE;
        }

        var defaults = intentDictionary.getResolutionDefaults(intent);
        Optional<TaskDomain> domainAffinity = Optional.empty();

        EngineProfile recommendedProfile = EngineProfile.QUALITY_PIPELINE;

        return new IntentDefaults(
                intent,
                defaults.defaultObjective(),
                defaults.preferredOutputNeeds(),
                defaults.defaultResponseShape(),
                domainAffinity.orElse(null),
                recommendedProfile
        );
    }
}
