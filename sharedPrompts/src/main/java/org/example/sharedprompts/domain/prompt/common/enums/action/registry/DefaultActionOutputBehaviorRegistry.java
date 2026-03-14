package org.example.sharedprompts.domain.prompt.common.enums.action.registry;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** stable key → OutputBehaviorType 매핑. 출력 정책은 enum이 아닌 이 레지스트리에서 관리. */
public final class DefaultActionOutputBehaviorRegistry implements ActionOutputBehaviorRegistry {

    /** stable key → OutputBehaviorType 매핑 */
    private final Map<String, OutputBehaviorType> keyToBehavior;

    public DefaultActionOutputBehaviorRegistry(Map<String, OutputBehaviorType> keyToBehavior) {
        this.keyToBehavior = Map.copyOf(Objects.requireNonNull(keyToBehavior, "keyToBehavior"));
    }

    @Override
    public Optional<OutputBehaviorType> getOutputBehavior(ActionTypeInterface actionType) {
        if (actionType == null || actionType.key() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(keyToBehavior.get(actionType.key()));
    }
}