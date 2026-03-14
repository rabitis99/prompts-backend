package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.metadata.ActionTypeMetadataLoader;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionOutputBehaviorRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.DefaultActionOutputBehaviorRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Safety test: every ActionType must have an output behavior in ActionOutputBehaviorRegistry.
 * Behavior is policy and is owned by the registry, not by the enum.
 */
@DisplayName("ActionType output behavior coverage")
class ActionTypeOutputBehaviorCoverageTest {

    @Test
    @DisplayName("every ActionType has output behavior in registry")
    void everyActionTypeHasOutputBehavior() {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getActionTypeEnums();
        ActionTypeRegistry registry = new ActionTypeRegistry(enumClasses);
        ActionOutputBehaviorRegistry outputBehaviorRegistry = new DefaultActionOutputBehaviorRegistry(
                ActionTypeMetadataLoader.loadKeyToOutputBehavior());
        for (ActionTypeInterface actionType : registry.getAll()) {
            assertThat(outputBehaviorRegistry.getOutputBehavior(actionType))
                    .as("ActionType " + actionType.key() + " must have outputBehavior in registry")
                    .isPresent();
        }
    }
}
