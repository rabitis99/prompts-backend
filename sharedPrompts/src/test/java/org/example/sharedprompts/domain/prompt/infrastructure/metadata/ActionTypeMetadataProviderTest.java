package org.example.sharedprompts.domain.prompt.infrastructure.metadata;

import org.example.sharedprompts.domain.prompt.common.enums.action.metadata.ActionTypeMetadataProvider;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.example.sharedprompts.domain.prompt.common.enums.action.metadata.test.InMemoryActionTypeMetadataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Metadata provider: load, fail-fast, registry without metadata, swappable provider, bootstrap independence.
 */
@DisplayName("ActionTypeMetadataProvider")
class ActionTypeMetadataProviderTest {

    @Test
    @DisplayName("ClasspathActionTypeMetadataProvider loads properties from classpath with full key coverage")
    void classpathProviderLoadsProperties() {
        ActionTypeMetadataProvider provider = new ClasspathActionTypeMetadataProvider();
        List<Class<? extends Enum<?>>> enums = DeserializerEnumTestUtils.getActionTypeEnums();
        ActionTypeRegistry registry = new ActionTypeRegistry(enums);
        for (var action : registry.getAll()) {
            assertThat(provider.getOutputBehavior(action.key()))
                    .as("Classpath provider must have output behavior for every ActionType: " + action.key())
                    .isPresent();
        }
    }

    @Test
    @DisplayName("ClasspathActionTypeMetadataProvider fails fast when resource is missing")
    void classpathProviderFailsFastWhenResourceMissing() {
        assertThatThrownBy(() -> new ClasspathActionTypeMetadataProvider(
                "nonexistent-output-behavior.properties",
                "nonexistent-domain.properties"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing action metadata resource")
                .hasMessageContaining("fail-fast");
    }

    @Test
    @DisplayName("ActionTypeRegistry works without any metadata")
    void registryWorksWithoutMetadata() {
        List<Class<? extends Enum<?>>> enums = DeserializerEnumTestUtils.getActionTypeEnums();
        ActionTypeRegistry registry = new ActionTypeRegistry(enums);
        assertThat(registry.getAll()).isNotEmpty();
        assertThat(registry.getByStableKey(registry.getAll().get(0).key())).isNotNull();
    }

    @Test
    @DisplayName("Metadata provider is swappable with in-memory provider")
    void metadataProviderIsSwappable() {
        InMemoryActionTypeMetadataProvider inMemory = new InMemoryActionTypeMetadataProvider()
                .putOutputBehavior("test.key", OutputBehaviorType.GENERAL_CONSULTATION)
                .putTaskDomain("test.key", TaskDomain.GENERAL);

        ActionTypeMetadataProvider provider = inMemory;
        assertThat(provider.getOutputBehavior("test.key")).contains(OutputBehaviorType.GENERAL_CONSULTATION);
        assertThat(provider.getTaskDomain("test.key")).contains(TaskDomain.GENERAL);
        assertThat(provider.getOutputBehavior("other")).isEmpty();
    }

    // Registry bootstrap independence is already covered by `registryWorksWithoutMetadata()`.
}
