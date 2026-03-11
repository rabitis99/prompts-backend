package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.output.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

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
