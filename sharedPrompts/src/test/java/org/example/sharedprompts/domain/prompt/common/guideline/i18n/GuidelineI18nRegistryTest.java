package org.example.sharedprompts.domain.prompt.common.guideline.i18n;

import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nText;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GuidelineI18nRegistry 단위 테스트")
class GuidelineI18nRegistryTest {

    @Test
    @DisplayName("등록 후 언어별 조회")
    void registerAndGet() {
        GuidelineI18nRegistry registry = new GuidelineI18nRegistry();
        registry.register("key1", I18nText.of("코", "En", "日"));
        assertThat(registry.get("key1", LanguageType.KOREAN)).isEqualTo("코");
        assertThat(registry.get("key1", LanguageType.ENGLISH)).isEqualTo("En");
        assertThat(registry.get("key1", LanguageType.JAPANESE)).isEqualTo("日");
    }

    @Test
    @DisplayName("누락된 번역 시 validate에서 에러")
    void validateDetectsMissingTranslation() {
        GuidelineI18nRegistry registry = new GuidelineI18nRegistry();
        registry.register("bad", I18nText.of("", "en", "ja"));
        List<String> errors = registry.validate();
        assertThat(errors).anyMatch(e -> e.contains("Missing ko"));
    }

    @Test
    @DisplayName("중복 키 등록 시 validate에서 에러")
    void validateDetectsDuplicateKey() {
        GuidelineI18nRegistry registry = new GuidelineI18nRegistry();
        registry.register("dup", I18nText.of("a", "b", "c"));
        registry.register("dup", I18nText.of("x", "y", "z"));
        List<String> errors = registry.validate();
        assertThat(errors).anyMatch(e -> e.contains("Duplicate") && e.contains("dup"));
    }

    @Test
    @DisplayName("존재하지 않는 키는 키 자체 반환")
    void missingKeyReturnsKey() {
        GuidelineI18nRegistry registry = new GuidelineI18nRegistry();
        assertThat(registry.get("nonexistent", LanguageType.KOREAN)).isEqualTo("nonexistent");
    }
}
