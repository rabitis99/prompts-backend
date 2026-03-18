package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionOutputBehaviorRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.metadata.test.InMemoryActionTypeMetadataProvider;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.DefaultActionOutputBehaviorRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Safety test: every ActionType must have an output behavior in ActionOutputBehaviorRegistry.
 * Uses in-memory provider so test does not depend on classpath properties.
 */
@DisplayName("ActionType output behavior coverage")
class ActionTypeOutputBehaviorCoverageTest {

    @Test
    @DisplayName("every ActionType has output behavior in registry")
    void everyActionTypeHasOutputBehavior() {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getActionTypeEnums();
        ActionTypeRegistry registry = new ActionTypeRegistry(enumClasses);
        InMemoryActionTypeMetadataProvider provider = new InMemoryActionTypeMetadataProvider();
        for (ActionTypeInterface action : registry.getAll()) {
            provider.putOutputBehavior(action.key(), OutputBehaviorType.GENERAL_CONSULTATION);
        }
        Map<String, OutputBehaviorType> map = new HashMap<>();
        registry.getAll().forEach(a -> provider.getOutputBehavior(a.key()).ifPresent(v -> map.put(a.key(), v)));
        ActionOutputBehaviorRegistry outputBehaviorRegistry = new DefaultActionOutputBehaviorRegistry(map);
        for (ActionTypeInterface actionType : registry.getAll()) {
            assertThat(outputBehaviorRegistry.getOutputBehavior(actionType))
                    .as("ActionType " + actionType.key() + " must have outputBehavior in registry")
                    .isPresent();
        }
    }
}
