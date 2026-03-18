package org.example.sharedprompts.domain.prompt.common.enums.action.resolver;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Legacy-only resolution: enum name and EnumName.CONSTANT format.
 * Does not resolve stable keys (those go through registry / DefaultActionTypeResolver).
 * Resolution order is taken from {@link OrderedActionTypeResolutionSource}; first match wins.
 */
public final class ActionTypeCompatibilityResolver {

    private final OrderedActionTypeResolutionSource resolutionOrder;

    public ActionTypeCompatibilityResolver(OrderedActionTypeResolutionSource resolutionOrder) {
        this.resolutionOrder = Objects.requireNonNull(resolutionOrder, "resolutionOrder");
    }

    /** Resolves legacy format only: (1) EnumSimpleName.CONSTANT, (2) legacy enum {@link Enum#name()}. No stable-key lookup. */
    public ActionTypeInterface resolve(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be null or blank");
        }
        String trimmed = value.trim();
        List<Class<? extends Enum<?>>> order = resolutionOrder.getResolutionOrder();
        validateResolutionOrder(order);
        ActionTypeInterface byDot = resolveByEnumDotConstant(trimmed, order);
        if (byDot != null) {
            return byDot;
        }
        return resolveByLegacyNameOnly(trimmed, order);
    }

    private static void validateResolutionOrder(List<Class<? extends Enum<?>>> order) {
        if (order == null) {
            throw new IllegalStateException("resolutionOrder.getResolutionOrder() must not return null");
        }
        if (order.isEmpty()) {
            throw new IllegalStateException("resolutionOrder.getResolutionOrder() must not return an empty list");
        }
        for (Class<? extends Enum<?>> enumClass : order) {
            if (enumClass == null) {
                throw new IllegalStateException("resolutionOrder.getResolutionOrder() must not contain null elements");
            }
        }
    }

    /** Legacy enum name only (Enum.valueOf). Stable key는 사용하지 않아 registry-first 계약과 중복되지 않음. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ActionTypeInterface resolveByLegacyNameOnly(String trimmed, List<Class<? extends Enum<?>>> enumClasses) {
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            try {
                Enum<?> constant = Enum.valueOf((Class) enumClass, trimmed);
                if (constant instanceof ActionTypeInterface action) {
                    return action;
                }
            } catch (IllegalArgumentException ignored) {
                // not this enum's name; try next
            }
        }
        String candidates = enumClasses.stream().map(Class::getSimpleName).collect(Collectors.joining(", "));
        throw new IllegalArgumentException(
                "Unknown enum value: '" + trimmed + "'. Tried legacy names in order: [" + candidates + "]"
        );
    }

    /** EnumSimpleName.CONSTANT 형식 우선 해석. 없으면 null. (enum 순서/simple name에 의존) */
    private ActionTypeInterface resolveByEnumDotConstant(String value, List<Class<? extends Enum<?>>> enumClasses) {
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            String enumSimpleName = enumClass.getSimpleName();
            String prefix = enumSimpleName + ".";
            if (!value.startsWith(prefix)) {
                continue;
            }
            String constantName = value.substring(prefix.length());
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                throw new IllegalStateException("Enum has no constants: " + enumClass.getName());
            }
            for (Enum<?> c : constants) {
                if (c.name().equals(constantName)) {
                    if (c instanceof ActionTypeInterface action) {
                        return action;
                    }
                    throw new IllegalStateException(
                            "Enum constant does not implement ActionTypeInterface: "
                                    + enumClass.getSimpleName() + "." + constantName
                    );
                }
            }
            // prefix matched but constant not found - likely a typo
            throw new IllegalArgumentException(
                    "Unknown constant '" + constantName + "' in enum " + enumSimpleName
                            + ". Available: " + Arrays.toString(Arrays.stream(constants).map(Enum::name).toArray(String[]::new))
            );
        }
        return null;
    }
}
