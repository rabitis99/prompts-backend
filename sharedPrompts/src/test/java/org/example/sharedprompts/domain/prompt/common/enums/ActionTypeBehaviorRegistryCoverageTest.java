package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Safety test: every ActionType registered in ActionTypeDeserializer.ACTION_TYPE_ENUMS
 * must have a mapping in ActionTypeBehaviorRegistry so that getOutputBehavior() never throws.
 * If a new ActionType is added to the deserializer but the registry is not updated, this test fails.
 *
 * <p>When adding a new ActionType enum: add it to ActionTypeDeserializer.ACTION_TYPE_ENUMS
 * and add a corresponding branch in ActionTypeBehaviorRegistry.resolveBehavior().</p>
 */
@DisplayName("ActionTypeBehaviorRegistry coverage")
class ActionTypeBehaviorRegistryCoverageTest {

    @Test
    @DisplayName("every ActionType in ACTION_TYPE_ENUMS has getOutputBehavior() that does not throw")
    void everyActionTypeInDeserializerHasMappedOutputBehavior() throws Exception {
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
                assertDoesNotThrow(
                        actionType::getOutputBehavior,
                        "ActionType " + enumClass.getSimpleName() + "." + constant.name()
                                + " must be mapped in ActionTypeBehaviorRegistry"
                );
            }
        }
    }
}
