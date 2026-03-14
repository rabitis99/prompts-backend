package org.example.sharedprompts.domain.prompt.common.enums.action.registry;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** ActionType 저장소. stable key 인덱스 + catalog 순서 유지. */
public final class ActionTypeRegistry {

    private final Map<String, ActionTypeInterface> byStableKey;
    private final List<ActionTypeInterface> allInCatalogOrder;

    /** catalog 순서대로 구성. */
    public ActionTypeRegistry(Iterable<Class<? extends Enum<?>>> enumClasses) {
        Objects.requireNonNull(enumClasses, "enumClasses");

        Map<String, ActionTypeInterface> keyMap = new HashMap<>();
        List<ActionTypeInterface> ordered = new ArrayList<>();

        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            validateActionTypeEnumClass(enumClass);

            String enumName = enumClass.getSimpleName();
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                throw new IllegalStateException("Enum class has no constants: " + enumClass.getName());
            }

            for (Enum<?> constant : constants) {
                ActionTypeInterface action = asActionType(constant, enumName);
                String key = action.key();
                if (key == null || key.isBlank()) {
                    throw new IllegalStateException(
                            "ActionType constant has null/blank key: " + enumName + "." + constant.name()
                    );
                }

                ActionGroup actionGroup = action.getActionGroup();
                if (actionGroup == null) {
                    throw new IllegalStateException(
                            "ActionType constant has null action group: "
                                    + enumName + "." + constant.name() + " (key=" + key + ")"
                    );
                }

                if (keyMap.put(key, action) != null) {
                    throw new IllegalStateException("Duplicate stable key: " + key);
                }

                ordered.add(action);
            }
        }

        this.byStableKey = Map.copyOf(keyMap);
        this.allInCatalogOrder = List.copyOf(ordered);
    }

    /** ActionType enum 계약 검증. */
    private static void validateActionTypeEnumClass(Class<? extends Enum<?>> enumClass) {
        if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) {
            throw new IllegalStateException(
                    "Catalog must contain only ActionType enums implementing ActionTypeInterface: "
                            + enumClass.getName()
            );
        }
    }

    /** enum 상수 → ActionTypeInterface 변환. */
    private static ActionTypeInterface asActionType(Enum<?> constant, String sourceClass) {
        if (!(constant instanceof ActionTypeInterface action)) {
            throw new IllegalStateException(
                    "Enum constant does not implement ActionTypeInterface: "
                            + sourceClass + "." + constant.name()
            );
        }
        return action;
    }

    /** stable key로 조회. */
    public ActionTypeInterface getByStableKey(String stableKey) {
        if (stableKey == null || stableKey.isBlank()) {
            return null;
        }
        return byStableKey.get(stableKey.trim());
    }

    /** 등록된 전체 ActionType을 catalog 순서로 반환. */
    public List<ActionTypeInterface> getAll() {
        return allInCatalogOrder;
    }
}