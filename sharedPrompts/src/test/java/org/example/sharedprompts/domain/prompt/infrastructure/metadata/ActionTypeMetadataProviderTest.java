package org.example.sharedprompts.domain.prompt.infrastructure.metadata;

import org.example.sharedprompts.domain.prompt.common.enums.action.metadata.ActionTypeMetadataProvider;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionOutputBehaviorRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.DefaultActionOutputBehaviorRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.example.sharedprompts.domain.prompt.common.enums.action.metadata.test.InMemoryActionTypeMetadataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Metadata provider: load, fail-fast, registry without metadata, swappable provider, bootstrap independence.
 */
@DisplayName("ActionTypeMetadataProvider")
class ActionTypeMetadataProviderTest {

    @Test
    @DisplayName("ClasspathActionTypeMetadataProvider loads properties from classpath")
    void classpathProviderLoadsProperties() {
        ActionTypeMetadataProvider provider = new ClasspathActionTypeMetadataProvider();
        // At least one key from catalog should have metadata if properties exist
        List<Class<? extends Enum<?>>> enums = DeserializerEnumTestUtils.getActionTypeEnums();
        ActionTypeRegistry registry = new ActionTypeRegistry(enums);
        boolean anyPresent = false;
        for (var action : registry.getAll()) {
            if (provider.getOutputBehavior(action.key()).isPresent()) {
                anyPresent = true;
                break;
            }
        }
        assertThat(anyPresent)
                .as("Classpath provider should return at least one output behavior when properties exist")
                .isTrue();
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

        assertThat(inMemory.getOutputBehavior("test.key")).contains(OutputBehaviorType.GENERAL_CONSULTATION);
        assertThat(inMemory.getTaskDomain("test.key")).contains(TaskDomain.GENERAL);
        assertThat(inMemory.getOutputBehavior("other")).isEmpty();

        Map<String, OutputBehaviorType> map = new HashMap<>();
        map.put("test.key", OutputBehaviorType.GENERAL_CONSULTATION);
        ActionOutputBehaviorRegistry registry = new DefaultActionOutputBehaviorRegistry(map);
        // Build registry from in-memory provider data
        ActionTypeMetadataProvider provider = inMemory;
        assertThat(provider.getOutputBehavior("test.key")).isPresent();
    }

    @Test
    @DisplayName("Registry bootstrap does not depend on metadata source")
    void registryBootstrapIndependentOfMetadataSource() {
        // Registry is built from catalog only; no metadata needed
        List<Class<? extends Enum<?>>> enums = DeserializerEnumTestUtils.getActionTypeEnums();
        ActionTypeRegistry registry = new ActionTypeRegistry(enums);
        assertThat(registry.getAll()).isNotEmpty();
        // Output/Domain registries are separate beans built from provider in Config;
        // registry itself has no reference to metadata
        assertThat(registry.getByStableKey(registry.getAll().get(0).key())).isNotNull();
    }
}
