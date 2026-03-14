package org.example.sharedprompts.domain.prompt.common.enums.action.registry;

import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ActionTypeRegistry fail-fast 및 API 동작 검증.
 */
@DisplayName("ActionTypeRegistry")
class ActionTypeRegistryTest {

    @Test
    @DisplayName("catalog에 ActionTypeInterface 미구현 enum이 있으면 생성 시 예외")
    void constructorThrowsWhenCatalogContainsNonActionTypeEnum() {
        List<Class<? extends Enum<?>>> mixed = List.of(
                DeserializerEnumTestUtils.getActionTypeEnums().get(0),
                java.lang.annotation.RetentionPolicy.class
        );
        assertThatThrownBy(() -> new ActionTypeRegistry(mixed))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ActionTypeInterface")
                .hasMessageContaining("RetentionPolicy");
    }

    @Test
    @DisplayName("getAll은 불변 리스트 반환")
    void getAllReturnsUnmodifiableList() {
        ActionTypeRegistry registry = new ActionTypeRegistry(DeserializerEnumTestUtils.getActionTypeEnums());
        var all = registry.getAll();
        assertThat(all).isNotNull();
        assertThat(all.size()).isEqualTo(registry.getAll().size());
        assertThatThrownBy(() -> all.clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
