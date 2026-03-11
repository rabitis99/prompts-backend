package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Safety test: every ActionType in ActionTypeCatalog must implement getOutputBehavior()
 * and return a non-null value. Behavior is owned by each enum; no external registry.
 */
@DisplayName("ActionType output behavior coverage")
class ActionTypeOutputBehaviorCoverageTest {

    @Test
    @DisplayName("every ActionType in ActionTypeCatalog has getOutputBehavior() returning non-null")
    void everyActionTypeHasOutputBehavior() {
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
                OutputBehaviorType behavior = actionType.getOutputBehavior();
                assertThat(behavior)
                        .as("ActionType " + enumClass.getSimpleName() + "." + constant.name() + " must define outputBehavior")
                        .isNotNull();
            }
        }
    }
}
