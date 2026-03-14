package org.example.sharedprompts.domain.prompt.common.enums.action.resolver;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.EnumResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 레거시 전용: enum 이름·EnumName.CONSTANT 형식만 해석. 신규는 stable key 사용. */
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

    /** 레거시 형식(enum name 또는 EnumName.CONSTANT)으로 해석. */
    public ActionTypeInterface resolve(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be null or blank");
        }
        String trimmed = value.trim();
        ActionTypeInterface byDot = resolveByEnumDotConstant(trimmed);
        if (byDot != null) {
            return byDot;
        }
        return EnumResolver.resolve(trimmed, enumClasses);
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
