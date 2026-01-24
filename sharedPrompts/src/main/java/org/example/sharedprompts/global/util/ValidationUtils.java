package org.example.sharedprompts.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationUtils {

    public static <T> T requireNonNull(T obj, String name) {
        if (obj == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return obj;
    }
}
