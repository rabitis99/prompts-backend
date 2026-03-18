package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.metadata.ActionTypeMetadataProvider;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.infrastructure.metadata.ClasspathActionTypeMetadataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Safety test: every ActionType must have output behavior in classpath metadata.
 * Verifies actual metadata source so missing entries are caught.
 */
@DisplayName("ActionType output behavior coverage")
class ActionTypeOutputBehaviorCoverageTest {

    @Test
    @DisplayName("every ActionType has output behavior in classpath metadata")
    void everyActionTypeHasOutputBehavior() {
        ActionTypeMetadataProvider provider = new ClasspathActionTypeMetadataProvider();
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getActionTypeEnums();
        ActionTypeRegistry registry = new ActionTypeRegistry(enumClasses);
        for (ActionTypeInterface actionType : registry.getAll()) {
            assertThat(provider.getOutputBehavior(actionType.key()))
                    .as("ActionType " + actionType.key() + " must have outputBehavior in classpath metadata")
                    .isPresent();
        }
    }
}
