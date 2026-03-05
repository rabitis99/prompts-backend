package org.example.sharedprompts.domain.prompt.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StableKeyedEnum 키 유일성 테스트")
class EnumKeyUniquenessTest {

    @Test
    @DisplayName("StyleType/ToneType의 key() 값은 전역에서 유일해야 한다")
    void stableKeysAreGloballyUnique() {
        Map<String, String> ownerByKey = new HashMap<>();

        assertUniqueKeysForEnum(ToneType.class, ownerByKey);
        assertUniqueKeysForEnum(StyleType.class, ownerByKey);
        assertUniqueKeysForEnum(PromptCategory.class, ownerByKey);
        assertUniqueKeysForEnum(ExperienceLevel.class, ownerByKey);
        assertUniqueKeysForEnum(TaskDomain.class, ownerByKey);
        // 향후 다른 StableKeyedEnum 타입들을 여기에 추가
    }

    private static void assertUniqueKeysForEnum(
            Class<? extends Enum<?>> enumClass,
            Map<String, String> ownerByKey
    ) {
        for (Enum<?> constant : enumClass.getEnumConstants()) {
            if (constant instanceof StableKeyedEnum keyed) {
                String key = keyed.key();
                String existingOwner = ownerByKey.putIfAbsent(key, enumClass.getName() + "." + constant.name());
                assertThat(existingOwner)
                        .withFailMessage("Duplicate stable enum key '%s' for %s and %s", key, existingOwner,
                                enumClass.getName() + "." + constant.name())
                        .isNull();
            }
        }
    }
}

