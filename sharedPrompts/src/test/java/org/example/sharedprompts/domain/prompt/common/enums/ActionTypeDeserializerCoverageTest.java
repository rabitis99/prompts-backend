package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.EnumResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Safety test: every enum class in ActionTypeDeserializer.ACTION_TYPE_ENUMS must implement
 * ActionTypeInterface, have valid keys, work with EnumResolver, and have no duplicate keys
 * across enums (to avoid ambiguous resolution).
 */
@DisplayName("ActionTypeDeserializer coverage and contract")
class ActionTypeDeserializerCoverageTest {

    @Test
    @DisplayName("every enum in ACTION_TYPE_ENUMS implements ActionTypeInterface and has valid key()")
    void everyEnumImplementsActionTypeInterfaceAndHasValidKey() throws Exception {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getActionTypeEnums();
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            assertThat(ActionTypeInterface.class.isAssignableFrom(enumClass))
                    .as("Enum " + enumClass.getName() + " must implement ActionTypeInterface")
                    .isTrue();

            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (Enum<?> constant : constants) {
                ActionTypeInterface actionType = (ActionTypeInterface) constant;
                String key = actionType.key();
                assertThat(key).isNotBlank();
                assertThat(key.trim()).isEqualTo(key);
            }
        }
    }

    @Test
    @DisplayName("each constant key() round-trips via EnumResolver without throwing")
    void eachConstantKeyRoundTripsViaEnumResolver() throws Exception {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getActionTypeEnums();
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) {
                continue;
            }
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (Enum<?> constant : constants) {
                ActionTypeInterface actionType = (ActionTypeInterface) constant;
                String key = actionType.key();
                assertDoesNotThrow(
                        () -> EnumResolver.resolve(key, enumClasses),
                        "Resolving key " + key + " for " + enumClass.getSimpleName() + " must not throw"
                );
            }
        }
    }

    @Test
    @DisplayName("no duplicate stable keys across ActionType enums (avoids ambiguous resolution)")
    void noDuplicateKeysAcrossActionTypeEnums() throws Exception {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getActionTypeEnums();
        Set<String> seenKeys = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) {
                continue;
            }
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (Enum<?> constant : constants) {
                ActionTypeInterface actionType = (ActionTypeInterface) constant;
                String key = actionType.key();
                if (key != null && !key.isBlank()) {
                    if (!seenKeys.add(key)) {
                        duplicates.add(key + " (second in " + enumClass.getSimpleName() + "." + constant.name() + ")");
                    }
                }
            }
        }
        assertThat(duplicates)
                .as("Duplicate stable keys would make EnumResolver resolution order-dependent and ambiguous")
                .isEmpty();
    }
}
