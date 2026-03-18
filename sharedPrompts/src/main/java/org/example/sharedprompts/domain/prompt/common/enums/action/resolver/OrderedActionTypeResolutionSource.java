package org.example.sharedprompts.domain.prompt.common.enums.action.resolver;

import java.util.List;

/**
 * Legacy compatibility resolution order only.
 * Supplies the ordered list of ActionType enum classes to try when resolving legacy names
 * (e.g. {@link Enum#name()}, EnumSimpleName.CONSTANT).
 * This is separate from "which ActionTypes are defined" (catalog); the order here
 * explicitly defines first-match priority for compatibility.
 */
public interface OrderedActionTypeResolutionSource {

    /**
     * Order to try when resolving a string via legacy compatibility.
     * First match wins. Not used for stable-key resolution.
     */
    List<Class<? extends Enum<?>>> getResolutionOrder();
}
