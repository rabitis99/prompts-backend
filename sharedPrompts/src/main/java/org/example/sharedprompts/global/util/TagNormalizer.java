package org.example.sharedprompts.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TagNormalizer {

    public static List<String> normalizeTags(List<String> tags) {
        if (tags == null) return List.of();

        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .map(tag -> tag.matches("^[a-zA-Z]+$") ? tag.toUpperCase() : tag)
                .distinct()
                .toList();
    }
}