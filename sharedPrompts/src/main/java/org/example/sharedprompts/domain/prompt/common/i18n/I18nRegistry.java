package org.example.sharedprompts.domain.prompt.common.i18n;

import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;

import java.util.*;

/**
 * Central i18n registry for enum metadata and shared prompt texts.
 *
 * <p>This registry is intentionally lightweight and framework-agnostic so that it
 * can be used in domain code, tests, and adapters without pulling in external
 * dependencies.</p>
 */
public final class I18nRegistry {

    private final Map<I18nKey, I18nText> resources = new HashMap<>();
    private final Set<I18nKey> duplicateKeys = new HashSet<>();

    /**
     * Global singleton registry for enum metadata.
     *
     * <p>Production code should normally interact with this instance. Tests may
     * still create isolated {@link I18nRegistry} instances when needed.</p>
     */
    private static final I18nRegistry GLOBAL = new I18nRegistry();

    public static I18nRegistry global() {
        return GLOBAL;
    }

    /**
     * Register a resource by key. Duplicate keys are tracked for validation.
     */
    public void register(I18nKey key, I18nText text) {
        if (key == null || text == null) return;
        if (resources.containsKey(key)) {
            duplicateKeys.add(key);
        }
        resources.put(key, text);
    }

    /**
     * Get text for key and language. Returns the key string if not found
     * (no framework dependency, safe for fallback).
     */
    public String lookup(I18nKey key, LanguageType lang) {
        if (key == null) {
            return "";
        }
        I18nText text = resources.get(key);
        if (text == null) return key.value();
        return text.byLang(lang);
    }

    /**
     * Validate: no missing translations (null/blank) and no duplicate keys.
     *
     * @return validation report; empty list means valid.
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        for (I18nKey key : duplicateKeys) {
            errors.add("Duplicate i18n key: " + key.value());
        }
        for (Map.Entry<I18nKey, I18nText> e : resources.entrySet()) {
            I18nText t = e.getValue();
            if (t == null) {
                errors.add("Null I18nText for key: " + e.getKey().value());
                continue;
            }
            if (isBlank(t.ko())) errors.add("Missing ko for key: " + e.getKey().value());
            if (isBlank(t.en())) errors.add("Missing en for key: " + e.getKey().value());
            if (isBlank(t.ja())) errors.add("Missing ja for key: " + e.getKey().value());
        }
        return Collections.unmodifiableList(errors);
    }

    public Map<I18nKey, I18nText> getResources() {
        return Collections.unmodifiableMap(resources);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

