package org.example.sharedprompts.domain.prompt.common.enums.action.resolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.catalog.ActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.action.catalog.DefaultActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 1차 리팩터링 구조 강제: stable key 우선, compatibility 순서 분리, catalog vs order 분리,
 * deserializer 주입(no holder), blank/null no hidden default, 해석 경로 명확성.
 */
@DisplayName("ActionType resolution structure (1st refactor)")
class ActionTypeResolutionStructureTest {

    private static ActionTypeCatalog catalog() {
        return new DefaultActionTypeCatalog();
    }

    private static ActionTypeResolver resolver(
            ActionTypeRegistry registry,
            OrderedActionTypeResolutionSource resolutionOrder) {
        return new DefaultActionTypeResolver(registry, new ActionTypeCompatibilityResolver(resolutionOrder));
    }

    @Nested
    @DisplayName("1. stable key takes precedence over compatibility")
    class StableKeyPrecedence {

        @Test
        void stableKeyResolvedFirst_compatibilityNotUsed() {
            ActionTypeCatalog catalog = catalog();
            OrderedActionTypeResolutionSource order = new DefaultOrderedActionTypeResolutionSource(catalog.getActionTypeEnumClasses());
            ActionTypeRegistry registry = new ActionTypeRegistry(catalog.getActionTypeEnumClasses());
            ActionTypeResolver r = resolver(registry, order);

            // Use a stable key that would also match as legacy in some enum
            List<ActionTypeInterface> all = registry.getAll();
            ActionTypeInterface first = all.get(0);
            String stableKey = first.key();

            assertThat(r.resolve(stableKey)).containsSame(first);
            // Same string resolved via stable key only; compatibility order does not change result when key exists
            assertThat(r.resolve(stableKey.trim())).containsSame(first);
        }
    }

    @Nested
    @DisplayName("2. compatibility resolution order is explicit policy")
    class CompatibilityOrderPolicy {

        @Test
        void resolutionOrderIsFromOrderedSource_notFromCatalogDirectly() {
            ActionTypeCatalog catalog = catalog();
            List<Class<? extends Enum<?>>> catalogList = catalog.getActionTypeEnumClasses();
            // Reverse order: compatibility tries enums in different order
            List<Class<? extends Enum<?>>> reversedOrder = new ArrayList<>(catalogList);
            Collections.reverse(reversedOrder);
            OrderedActionTypeResolutionSource reversedSource = new DefaultOrderedActionTypeResolutionSource(reversedOrder);
            ActionTypeRegistry registry = new ActionTypeRegistry(catalogList);
            ActionTypeResolver r = resolver(registry, reversedSource);

            // Resolve by legacy name only (no stable key collision). Result depends on order.
            String legacyName = org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity.ProductivityActionType.WORKFLOW_OPTIMIZATION.name();
            assertThat(r.resolve(legacyName)).isPresent();
            // Order is explicitly from reversedSource; test just asserts order is used
            assertThat(reversedSource.getResolutionOrder()).isEqualTo(reversedOrder);
        }
    }

    @Nested
    @DisplayName("3. catalog definition set vs compatibility order are separate")
    class CatalogVsCompatibilitySeparation {

        @Test
        void catalogProvidesDefinitionSet_compatibilityOrderCanDiffer() {
            ActionTypeCatalog catalog = catalog();
            List<Class<? extends Enum<?>>> definitionSet = catalog.getActionTypeEnumClasses();
            // Same set, different order for compatibility
            List<Class<? extends Enum<?>>> reversedOrder = new ArrayList<>(definitionSet);
            Collections.reverse(reversedOrder);
            OrderedActionTypeResolutionSource order = new DefaultOrderedActionTypeResolutionSource(reversedOrder);
            ActionTypeRegistry registry = new ActionTypeRegistry(definitionSet);

            assertThat(registry.getAll()).hasSize(definitionSet.stream()
                    .mapToInt(c -> c.getEnumConstants() != null ? c.getEnumConstants().length : 0).sum());
            assertThat(order.getResolutionOrder()).isEqualTo(reversedOrder);
            assertThat(definitionSet).containsExactlyElementsOf(catalog.getActionTypeEnumClasses());
            // Catalog list unchanged; order is separate
            assertThat(catalog.getActionTypeEnumClasses()).isEqualTo(definitionSet);
        }
    }

    @Nested
    @DisplayName("4. deserializer works without static holder")
    class DeserializerNoHolder {

        @Test
        void deserializerUsesInjectedResolver_only() throws JsonProcessingException {
            ActionTypeCatalog catalog = catalog();
            OrderedActionTypeResolutionSource order = new DefaultOrderedActionTypeResolutionSource(catalog.getActionTypeEnumClasses());
            ActionTypeRegistry registry = new ActionTypeRegistry(catalog.getActionTypeEnumClasses());
            ActionTypeResolver r = resolver(registry, order);
            ActionTypeDeserializer deserializer = new ActionTypeDeserializer(r);

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.databind.module.SimpleModule() {{
                addDeserializer(ActionTypeInterface.class, deserializer);
            }});

            String json = "\"ACTION.PRODUCTIVITY.WORKFLOW_OPTIMIZATION\"";
            ActionTypeInterface value = mapper.readValue(json, ActionTypeInterface.class);
            assertThat(value).isNotNull();
            assertThat(value.key()).isEqualTo("ACTION.PRODUCTIVITY.WORKFLOW_OPTIMIZATION");
        }
    }

    @Nested
    @DisplayName("5. blank/null: no hidden default")
    class NoHiddenDefaultForBlank {

        @Test
        void resolverRejectsBlank_throws() {
            ActionTypeCatalog catalog = catalog();
            OrderedActionTypeResolutionSource order = new DefaultOrderedActionTypeResolutionSource(catalog.getActionTypeEnumClasses());
            ActionTypeRegistry registry = new ActionTypeRegistry(catalog.getActionTypeEnumClasses());
            ActionTypeResolver r = resolver(registry, order);

            assertThatThrownBy(() -> r.resolve(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> r.resolve("")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> r.resolve("   ")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deserializerRejectsBlank_throws() throws Exception {
            ActionTypeCatalog catalog = catalog();
            OrderedActionTypeResolutionSource order = new DefaultOrderedActionTypeResolutionSource(catalog.getActionTypeEnumClasses());
            ActionTypeRegistry registry = new ActionTypeRegistry(catalog.getActionTypeEnumClasses());
            ActionTypeResolver r = resolver(registry, order);
            ActionTypeDeserializer deserializer = new ActionTypeDeserializer(r);

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.databind.module.SimpleModule() {{
                addDeserializer(ActionTypeInterface.class, deserializer);
            }});

            assertThatThrownBy(() -> mapper.readValue("\"\"", ActionTypeInterface.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or blank");
        }
    }

    @Nested
    @DisplayName("6. resolution paths: stable key, legacy name, EnumName.CONSTANT")
    class ResolutionPaths {

        @Test
        void stableKey_resolvedViaRegistry() {
            ActionTypeCatalog catalog = catalog();
            OrderedActionTypeResolutionSource order = new DefaultOrderedActionTypeResolutionSource(catalog.getActionTypeEnumClasses());
            ActionTypeRegistry registry = new ActionTypeRegistry(catalog.getActionTypeEnumClasses());
            ActionTypeResolver r = resolver(registry, order);

            String key = "ACTION.PRODUCTIVITY.WORKFLOW_OPTIMIZATION";
            assertThat(registry.getByStableKey(key)).isNotNull();
            assertThat(r.resolve(key)).isPresent();
        }

        @Test
        void legacyName_resolvedViaCompatibility() {
            ActionTypeCatalog catalog = catalog();
            OrderedActionTypeResolutionSource order = new DefaultOrderedActionTypeResolutionSource(catalog.getActionTypeEnumClasses());
            ActionTypeRegistry registry = new ActionTypeRegistry(catalog.getActionTypeEnumClasses());
            ActionTypeResolver r = resolver(registry, order);

            String legacyName = "WORKFLOW_OPTIMIZATION";
            assertThat(r.resolve(legacyName)).isPresent();
        }

        @Test
        void enumDotConstant_resolvedViaCompatibility() {
            ActionTypeCatalog catalog = catalog();
            OrderedActionTypeResolutionSource order = new DefaultOrderedActionTypeResolutionSource(catalog.getActionTypeEnumClasses());
            ActionTypeRegistry registry = new ActionTypeRegistry(catalog.getActionTypeEnumClasses());
            ActionTypeResolver r = resolver(registry, order);

            String qualified = "ProductivityActionType.WORKFLOW_OPTIMIZATION";
            assertThat(r.resolve(qualified)).isPresent();
        }
    }

    @Nested
    @DisplayName("7. compatibility order change does not change catalog definition meaning")
    class CompatibilityOrderIndependentOfCatalog {

        @Test
        void changingResolutionOrder_doesNotChangeCatalogContent() {
            ActionTypeCatalog catalog = catalog();
            List<Class<? extends Enum<?>>> defs = catalog.getActionTypeEnumClasses();
            List<Class<? extends Enum<?>>> orderA = defs;
            List<Class<? extends Enum<?>>> orderB = new ArrayList<>(defs);
            Collections.reverse(orderB);

            OrderedActionTypeResolutionSource sourceA = new DefaultOrderedActionTypeResolutionSource(orderA);
            OrderedActionTypeResolutionSource sourceB = new DefaultOrderedActionTypeResolutionSource(orderB);

            assertThat(sourceA.getResolutionOrder()).isNotEqualTo(sourceB.getResolutionOrder());
            assertThat(catalog.getActionTypeEnumClasses()).containsExactlyElementsOf(defs);
            // Catalog still same set
            ActionTypeRegistry registry = new ActionTypeRegistry(catalog.getActionTypeEnumClasses());
            assertThat(registry.getAll()).hasSize(
                    defs.stream().mapToInt(c -> c.getEnumConstants() == null ? 0 : c.getEnumConstants().length).sum());
        }
    }
}
