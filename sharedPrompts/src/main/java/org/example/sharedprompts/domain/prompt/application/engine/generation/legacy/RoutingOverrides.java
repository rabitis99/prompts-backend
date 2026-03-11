package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.role.core.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.output.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import org.example.sharedprompts.domain.prompt.common.enums.role.metadata.DomainRoleType;

import java.util.Collections;
import java.util.List;

/** 룰 엔진 적용 결과: 선택된 룰의 override 값들 */
public record RoutingOverrides(
        PromptObjective objectiveOverride,
        OutputNeeds outputNeedsOverride,
        ResponseShape responseShapeOverride,
        TaskDomain domainOverride,
        EngineProfile engineProfileOverride,
        CoreRoleType coreRoleOverride,
        DomainRoleType domainRoleOverride,
        List<String> appliedRuleIds
) {

    public RoutingOverrides {
        appliedRuleIds = appliedRuleIds != null ? List.copyOf(appliedRuleIds) : Collections.emptyList();
    }

    public static RoutingOverrides empty() {
        return new RoutingOverrides(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Collections.emptyList()
        );
    }
}
