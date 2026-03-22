package org.example.sharedprompts.domain.prompt.common.enums.action.canonical;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

import java.util.Objects;
import java.util.Optional;

/** ActionType → ActionGroup(capability) 매핑 레지스트리. */
public interface CanonicalActionRegistry {

    /**
     * Permissive lookup: empty when {@code actionType} is null or no group can be resolved
     * (e.g. unknown stable key with no definition). Prefer {@link #requireActionGroup(ActionTypeInterface)}
     * for curated policy/seed lists where absence is a configuration error.
     */
    Optional<ActionGroup> toCanonical(ActionTypeInterface actionType);

    /**
     * Permissive: stable key may be missing from {@link org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry}.
     */
    Optional<ActionGroup> findByKey(String stableKey);

    /**
     * Strict: curated taxonomy / seed / policy paths must resolve a capability. Failure is an
     * {@link IllegalArgumentException} with the action stable key in the message.
     */
    default ActionGroup requireActionGroup(ActionTypeInterface actionType) {
        Objects.requireNonNull(actionType, "actionType");
        return toCanonical(actionType)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Missing ActionGroup for action type stableKey="
                                                + actionType.key()
                                                + " (class="
                                                + actionType.getClass().getName()
                                                + "). Definition-first resolution failed; ensure getActionGroup() is non-null or the stable key is registered."));
    }

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