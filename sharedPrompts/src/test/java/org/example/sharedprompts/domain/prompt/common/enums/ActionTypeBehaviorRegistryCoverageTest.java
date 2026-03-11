package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Safety test: every ActionType registered in ActionTypeDeserializer.ACTION_TYPE_ENUMS
 * must have a mapping in ActionTypeBehaviorRegistry so that getOutputBehavior() never throws.
 * If a new ActionType is added to the deserializer but the registry is not updated, this test fails.
 */
@DisplayName("ActionTypeBehaviorRegistry coverage")
class ActionTypeBehaviorRegistryCoverageTest {

    @SuppressWarnings("unchecked")
    private static List<Class<? extends Enum<?>>> getActionTypeEnums() throws Exception {
        Field field = ActionTypeDeserializer.class.getDeclaredField("ACTION_TYPE_ENUMS");
        field.setAccessible(true);
        return (List<Class<? extends Enum<?>>>) field.get(null);
    }

    @Test
    @DisplayName("every ActionType in ACTION_TYPE_ENUMS has getOutputBehavior() that does not throw")
    void everyActionTypeInDeserializerHasMappedOutputBehavior() throws Exception {
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
                assertDoesNotThrow(
                        actionType::getOutputBehavior,
                        "ActionType " + enumClass.getSimpleName() + "." + constant.name()
                                + " must be mapped in ActionTypeBehaviorRegistry"
                );
            }
        }
    }
}
