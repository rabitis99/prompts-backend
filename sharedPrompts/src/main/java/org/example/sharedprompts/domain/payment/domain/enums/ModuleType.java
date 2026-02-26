package org.example.sharedprompts.domain.payment.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ModuleType {
    TEXT("텍스트"),
    EMAIL("이메일"),
    BLOG("블로그"),
    IMAGE("이미지"),
    DOCUMENT("문서"),
    LITERARY("문학"),
    UNKNOWN("미지정");

    private final String description;

    public static ModuleType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ModuleType t : values()) {
            if (t.name().equalsIgnoreCase(value.trim())) {
                return t;
            }
        }
        return null;
    }
}
