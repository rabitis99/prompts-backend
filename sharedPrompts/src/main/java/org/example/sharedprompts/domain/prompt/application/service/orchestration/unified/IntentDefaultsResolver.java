package org.example.sharedprompts.domain.prompt.application.service.orchestration.unified;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Intent 메타데이터 기반 기본 라우팅 값 결정기.
 *
 * <p>if-else 분기를 최소화하고 {@link ActionIntent} 에 정의된 메타데이터만을 사용한다.</p>
 */
@Component
@RequiredArgsConstructor
public class IntentDefaultsResolver {

    public IntentDefaults resolve(UnifiedGeneratePromptCommand command) {
        ActionIntent intent = command.intent();
        if (intent == null) {
            // 외부 계약에서 null 이 올 수 있으므로 안전하게 기본값 GENERATE 로 보정한다.
            intent = ActionIntent.GENERATE;
        }

        PromptObjective objective = intent.getDefaultObjective();
        OutputNeeds outputNeeds = intent.getPreferredOutputNeeds();
        ResponseShape responseShape = intent.getDefaultResponseShape();
        Optional<TaskDomain> domainAffinity = intent.getDomainAffinity();

        // EngineProfile 은 현재는 품질 파이프라인을 기본으로 사용한다.
        // Fast/JsonStrict 는 별도 Rule 또는 OutputContractPlanner 에서 강제한다.
        EngineProfile recommendedProfile = EngineProfile.QUALITY_PIPELINE;

        return new IntentDefaults(
                intent,
                objective,
                outputNeeds,
                responseShape,
                domainAffinity.orElse(null),
                recommendedProfile
        );
    }
}

