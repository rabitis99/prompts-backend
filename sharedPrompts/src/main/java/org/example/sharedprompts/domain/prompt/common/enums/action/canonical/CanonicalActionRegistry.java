package org.example.sharedprompts.domain.prompt.common.enums.action.canonical;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

import java.util.Optional;

/** ActionType → ActionGroup(capability) 매핑 레지스트리. */
public interface CanonicalActionRegistry {

    /** ActionType을 ActionGroup으로 변환. */
    Optional<ActionGroup> toCanonical(ActionTypeInterface actionType);

    /** stable key로 ActionGroup 조회. */
    Optional<ActionGroup> findByKey(String stableKey);

    /** 두 ActionType이 동일 capability(ActionGroup)인지 비교. */
    default boolean sameCanonicalCapability(ActionTypeInterface a, ActionTypeInterface b) {
        if (a == null || b == null) {
            return false;
        }

        Optional<ActionGroup> ca = toCanonical(a);
        Optional<ActionGroup> cb = toCanonical(b);

        return ca.isPresent() && cb.isPresent() && ca.get() == cb.get();
    }
}