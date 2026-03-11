package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;

import java.util.List;

/** 출력 계약(Objective/OutputNeeds) 최종 결정 */
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
