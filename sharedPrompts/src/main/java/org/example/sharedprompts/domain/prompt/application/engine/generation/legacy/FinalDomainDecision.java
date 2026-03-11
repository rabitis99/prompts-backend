package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

import java.util.List;
import java.util.Objects;

/** 최종 도메인 결정 및 사유 */
public record FinalDomainDecision(
        TaskDomain domain,
        List<String> reasons
) {
    public FinalDomainDecision {
        domain = Objects.requireNonNull(domain, "domain must not be null");
        reasons = reasons != null ? List.copyOf(reasons) : List.of();
    }
}
