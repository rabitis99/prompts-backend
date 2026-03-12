package org.example.sharedprompts.domain.prompt.common.enums.action.canonical;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

import java.util.Optional;

/**
 * Maps concrete ActionType values to canonical actions for resolution and profile logic.
 * Internal semantic resolution uses canonical action; stable keys remain for API and deserialization.
 */
public interface CanonicalActionRegistry {

    /**
     * Resolve an action type to its canonical capability id.
     *
     * @param actionType concrete action (e.g. from request); may be null
     * @return the canonical action id, or empty if action is null or unmapped (should not occur if catalog is complete)
     */
    Optional<CanonicalActionId> toCanonical(ActionTypeInterface actionType);

    /**
     * Resolve by stable key (e.g. after deserialization).
     *
     * @param stableKey action key string
     * @return the canonical action id, or empty if key is unknown
     */
    Optional<CanonicalActionId> findByKey(String stableKey);

    /**
     * Returns true if the two actions are the same capability (canonical equality).
     * Handles nulls: null is not equal to any action.
     */
    default boolean sameCanonicalCapability(ActionTypeInterface a, ActionTypeInterface b) {
        if (a == null || b == null) {
            return false;
        }
        Optional<CanonicalActionId> ca = toCanonical(a);
        Optional<CanonicalActionId> cb = toCanonical(b);
        return ca.isPresent() && cb.isPresent() && ca.get() == cb.get();
    }
}
