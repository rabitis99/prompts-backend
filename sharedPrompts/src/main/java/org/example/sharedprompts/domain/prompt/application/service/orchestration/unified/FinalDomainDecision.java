package org.example.sharedprompts.domain.prompt.application.service.orchestration.unified;

import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

import java.util.List;

/**
 * 최종 도메인 결정 + 사유.
 */
public record FinalDomainDecision(
        TaskDomain domain,
        List<String> reasons
) {
    public FinalDomainDecision {
        reasons = reasons != null ? List.copyOf(reasons) : List.of();
    }
}

