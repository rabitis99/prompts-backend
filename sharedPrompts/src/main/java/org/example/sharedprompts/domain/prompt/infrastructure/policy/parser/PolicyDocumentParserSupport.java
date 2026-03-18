package org.example.sharedprompts.domain.prompt.infrastructure.policy.parser;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.PolicyDocumentMetadata;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Shared helpers for parsing raw maps into policy document fields.
 */
final class PolicyDocumentParserSupport {

    private PolicyDocumentParserSupport() {}

    static String stringOr(Map<String, Object> raw, String key, String defaultValue) {
        Object v = raw.get(key);
        if (v == null) return defaultValue;
        String s = v.toString().trim();
        return s.isEmpty() ? defaultValue : s;
    }

    @SuppressWarnings("unchecked")
    static PolicyDocumentMetadata metadata(Map<String, Object> raw) {
        Object m = raw.get("metadata");
        if (m == null || !(m instanceof Map)) return PolicyDocumentMetadata.empty();
        Map<String, Object> map = (Map<String, Object>) m;
        String desc = stringOr(map, "description", "");
        String sourceType = stringOr(map, "sourceType", "unknown");
        String tag = stringOr(map, "experimentTag", null);
        return new PolicyDocumentMetadata(desc, sourceType, tag != null ? Optional.of(tag) : Optional.empty());
    }

    static Optional<String> sourceInfo(Map<String, Object> raw) {
        Object v = raw.get("sourceInfo");
        if (v == null) return Optional.empty();
        String s = v.toString().trim();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    /** Extract map of context key -> list of strings from "preferences" or "rules". */
    @SuppressWarnings("unchecked")
    static Map<String, List<String>> rulesOrPreferences(Map<String, Object> raw) {
        Object rules = raw.get("rules");
        if (rules instanceof Map) return normalizeStringListMap((Map<?, ?>) rules);
        Object prefs = raw.get("preferences");
        if (prefs instanceof Map) return normalizeStringListMap((Map<?, ?>) prefs);
        return Map.of();
    }

    static Map<String, List<String>> normalizeStringListMap(Map<?, ?> map) {
        if (map == null) return Map.of();
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!(e.getKey() instanceof String)) continue;
            String key = (String) e.getKey();
            List<String> list = toListOfStrings(e.getValue());
            out.put(key, List.copyOf(list));
        }
        return Map.copyOf(out);
    }

    static List<String> toListOfStrings(Object value) {
        if (value == null) return List.of();
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return List.of(value.toString().trim());
    }

    @SuppressWarnings("unchecked")
    static Map<String, String> stringMap(Map<String, Object> raw, String key) {
        Object v = raw.get(key);
        if (v == null || !(v instanceof Map)) return Map.of();
        Map<?, ?> m = (Map<?, ?>) v;
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (e.getKey() instanceof String && e.getValue() != null) {
                out.put((String) e.getKey(), e.getValue().toString().trim());
            }
        }
        return Map.copyOf(out);
    }
}
