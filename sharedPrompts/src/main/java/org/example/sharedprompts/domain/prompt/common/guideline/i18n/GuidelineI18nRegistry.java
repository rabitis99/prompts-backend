package org.example.sharedprompts.domain.prompt.common.guideline.i18n;

import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nText;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 가이드라인 전용 i18n 레지스트리 (문자열 키 기반).
 *
 * <p>enum 메타데이터·공용 텍스트용 레지스트리는 {@link org.example.sharedprompts.domain.prompt.common.i18n.I18nRegistry}를 사용한다.
 * 이 클래스는 가이드라인 규칙/도메인 해석 등 guideline 패키지 내부에서만 사용하는 번역 맵이다.</p>
 */
public final class GuidelineI18nRegistry {

    private final Map<String, I18nText> resources = new HashMap<>();
    private final Set<String> duplicateKeys = new HashSet<>();

    /**
     * Register a resource by key. Duplicate keys are tracked for validation.
     */
    public void register(String key, I18nText text) {
        if (key == null || key.isBlank()) return;
        if (resources.containsKey(key)) {
            duplicateKeys.add(key);
        }
        resources.put(key, text);
    }

    /**
     * Get text for key and language. Returns key if not found (no framework dependency).
     */
    public String get(String key, LanguageType lang) {
        if (key == null || key.isBlank()) return "";
        I18nText text = resources.get(key);
        if (text == null) return key;
        String resolved = text.byLang(lang);
        return isBlank(resolved) ? key : resolved;
    }

    /**
     * Validate: no missing translations (null/blank) and no duplicate keys.
     * @return validation report; empty list means valid.
     */
    public java.util.List<String> validate() {
        java.util.List<String> errors = new java.util.ArrayList<>();
        for (String key : duplicateKeys) {
            errors.add("Duplicate i18n key: " + key);
        }
        for (Map.Entry<String, I18nText> e : resources.entrySet()) {
            I18nText t = e.getValue();
            if (t == null) {
                errors.add("Null I18nText for key: " + e.getKey());
                continue;
            }
            if (isBlank(t.ko())) errors.add("Missing ko for key: " + e.getKey());
            if (isBlank(t.en())) errors.add("Missing en for key: " + e.getKey());
            if (isBlank(t.ja())) errors.add("Missing ja for key: " + e.getKey());
        }
        return Collections.unmodifiableList(errors);
    }

    public Map<String, I18nText> getResources() {
        return Collections.unmodifiableMap(resources);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
