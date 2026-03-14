package org.example.sharedprompts.domain.prompt.common.enums.action.registry;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;

import java.util.Optional;

/** ActionType별 출력 정책(OutputBehavior) 조회 레지스트리. */
public interface ActionOutputBehaviorRegistry {

    /** ActionType에 대응하는 출력 정책 반환. */
    Optional<OutputBehaviorType> getOutputBehavior(ActionTypeInterface actionType);
}