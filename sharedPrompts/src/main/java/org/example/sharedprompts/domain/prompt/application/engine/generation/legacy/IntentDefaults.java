package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

/** Intent 기반 기본 라우팅 값 */
public record IntentDefaults(
        ActionIntent intent,
        PromptObjective objective,
        OutputNeeds outputNeeds,
        ResponseShape responseShape,
        TaskDomain domainAffinity,
        EngineProfile recommendedEngineProfile
) {
}
