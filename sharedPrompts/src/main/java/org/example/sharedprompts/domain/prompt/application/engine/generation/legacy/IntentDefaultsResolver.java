package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDictionary;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/** legacy 라우팅용 Intent 기본값 */
@Component
public class IntentDefaultsResolver {

    public IntentDefaults resolve(UnifiedGeneratePromptCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ActionIntent intent = command.intent();
        if (intent == null) {
            intent = ActionIntent.GENERATE;
        }

        var defaults = IntentDictionary.getResolutionDefaults(intent);
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
