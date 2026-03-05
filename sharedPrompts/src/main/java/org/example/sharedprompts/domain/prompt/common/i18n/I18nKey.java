package org.example.sharedprompts.domain.prompt.common.i18n;

import java.util.Objects;

/**
 * Type-safe i18n key used for enum metadata, guidelines, and UI text.
 *
 * <p>This is a thin wrapper around a String value, but:
 * <ul>
 *   <li>makes call sites explicit about using i18n keys instead of raw strings</li>
 *   <li>allows build-time and test-time validation of key uniqueness</li>
 *   <li>acts as a stable identifier that should not change once published</li>
 * </ul>
 */
public record I18nKey(String value) {

    public I18nKey {
        Objects.requireNonNull(value, "i18n key must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("i18n key must not be blank");
        }
    }

    public static I18nKey of(String value) {
        return new I18nKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

