package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.EnumResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Safety test: every enum class in ActionTypeDeserializer.ACTION_TYPE_ENUMS must implement
 * ActionTypeInterface, have valid keys, and work with EnumResolver so deserializer and enum contract stay consistent.
 */
@DisplayName("ActionTypeDeserializer coverage and contract")
class ActionTypeDeserializerCoverageTest {

    @SuppressWarnings("unchecked")
    private static List<Class<? extends Enum<?>>> getActionTypeEnums() throws Exception {
        Field field = ActionTypeDeserializer.class.getDeclaredField("ACTION_TYPE_ENUMS");
        field.setAccessible(true);
        return (List<Class<? extends Enum<?>>>) field.get(null);
    }

    @Test
    @DisplayName("every enum in ACTION_TYPE_ENUMS implements ActionTypeInterface and has valid key()")
    void everyEnumImplementsActionTypeInterfaceAndHasValidKey() throws Exception {
        List<Class<? extends Enum<?>>> enumClasses = getActionTypeEnums();
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
        List<Class<? extends Enum<?>>> enumClasses = getActionTypeEnums();
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
}
