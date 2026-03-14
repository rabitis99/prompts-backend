package org.example.sharedprompts.domain.prompt.common.enums.action.resolver;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Legacy-only resolution: enum name and EnumName.CONSTANT format.
 * Does not resolve stable keys (those go through registry / DefaultActionTypeResolver).
 * Fallback uses Enum.valueOf per enum class, so only legacy names are accepted here.
 */
public final class ActionTypeCompatibilityResolver {

    /** ActionTypeInterface 구현 enum만 보관. 생성 시 검증 후 방어적 복사. */
    private final List<Class<? extends Enum<?>>> enumClasses;

    public ActionTypeCompatibilityResolver(List<Class<? extends Enum<?>>> enumClasses) {
        Objects.requireNonNull(enumClasses, "enumClasses");
        List<Class<? extends Enum<?>>> copy = new ArrayList<>(enumClasses);
        for (Class<? extends Enum<?>> enumClass : copy) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) {
                throw new IllegalStateException(
                        "ActionTypeCompatibilityResolver requires ActionTypeInterface enum classes only. Invalid: "
                                + enumClass.getName()
                );
            }
        }
        this.enumClasses = List.copyOf(copy);
    }

    /** Resolves legacy format only: (1) EnumSimpleName.CONSTANT, (2) legacy enum {@link Enum#name()}. No stable-key lookup. */
    public ActionTypeInterface resolve(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be null or blank");
        }
        String trimmed = value.trim();
        ActionTypeInterface byDot = resolveByEnumDotConstant(trimmed);
        if (byDot != null) {
            return byDot;
        }
        return resolveByLegacyNameOnly(trimmed);
    }

    /** Legacy enum name only (Enum.valueOf). Stable key는 사용하지 않아 registry-first 계약과 중복되지 않음. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ActionTypeInterface resolveByLegacyNameOnly(String trimmed) {
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
    private ActionTypeInterface resolveByEnumDotConstant(String value) {
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
        }
        return null;
    }
}
