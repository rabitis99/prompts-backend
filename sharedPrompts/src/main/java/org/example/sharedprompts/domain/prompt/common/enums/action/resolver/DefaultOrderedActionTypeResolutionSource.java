package org.example.sharedprompts.domain.prompt.common.enums.action.resolver;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of compatibility resolution order.
 * Holds an explicit ordered list; used only by {@link ActionTypeCompatibilityResolver}.
 * Order is independent of catalog "definition set" semantics.
 */
public final class DefaultOrderedActionTypeResolutionSource implements OrderedActionTypeResolutionSource {

    private final List<Class<? extends Enum<?>>> resolutionOrder;

    public DefaultOrderedActionTypeResolutionSource(List<Class<? extends Enum<?>>> resolutionOrder) {
        Objects.requireNonNull(resolutionOrder, "resolutionOrder");
        List<Class<? extends Enum<?>>> copy = new ArrayList<>(resolutionOrder);
        for (Class<? extends Enum<?>> enumClass : copy) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) {
                throw new IllegalStateException(
                        "OrderedActionTypeResolutionSource requires ActionTypeInterface enum classes only. Invalid: "
                                + enumClass.getName());
            }
        }
        this.resolutionOrder = List.copyOf(copy);
    }

    @Override
    public List<Class<? extends Enum<?>>> getResolutionOrder() {
        return resolutionOrder;
    }
}
