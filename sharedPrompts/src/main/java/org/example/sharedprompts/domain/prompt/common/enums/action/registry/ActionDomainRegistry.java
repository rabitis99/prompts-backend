package org.example.sharedprompts.domain.prompt.common.enums.action.registry;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

/** ActionType별 TaskDomain 조회 레지스트리. */
public interface ActionDomainRegistry {

    /** ActionType에 대응하는 TaskDomain 반환. */
    Optional<TaskDomain> getTaskDomain(ActionTypeInterface actionType);
}