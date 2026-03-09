package org.example.sharedprompts.domain.prompt.application.service.orchestration.legacy;

import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;

import java.util.List;

/**
 * 출력 계약(Contract)에 대한 최종 결정.
 */
public record ContractDecision(
        PromptObjective objective,
        OutputNeeds outputNeeds,
        boolean schemaRequired,
        List<String> reasons
) {
    public ContractDecision {
        reasons = reasons != null ? List.copyOf(reasons) : List.of();
    }
}

