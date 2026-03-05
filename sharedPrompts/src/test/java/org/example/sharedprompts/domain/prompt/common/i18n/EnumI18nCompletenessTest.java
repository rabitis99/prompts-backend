package org.example.sharedprompts.domain.prompt.common.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Enum i18n 레지스트리 완전성 테스트")
class EnumI18nCompletenessTest {

    @Test
    @DisplayName("등록된 enum i18n 리소스는 누락된 번역이 없어야 한다")
    void enumI18nHasNoMissingTranslations() {
        I18nRegistry registry = I18nRegistry.global();
        // enum i18n 마이그레이션 이전 단계에서는 비어 있을 수 있으므로,
        // 리소스가 존재할 때만 검증을 수행한다.
        if (registry.getResources().isEmpty()) {
            return;
        }
        assertThat(registry.validate()).isEmpty();
    }
}

