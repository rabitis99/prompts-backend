package org.example.sharedprompts.domain.prompt.common.guideline.i18n;

import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
public final class I18nRegistry {

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
        I18nText text = resources.get(key);
        if (text == null) return key;
        return text.byLang(lang);
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
