package org.example.sharedprompts.domain.prompt.infrastructure.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ActionRecommendationPreferenceSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads action recommendation preferences from a classpath JSON file.
 * Key format: "CATEGORY+INTENT" (e.g. WRITING+GENERATE); value: ordered list of action stable keys.
 * Structure is extensible (e.g. top-level "preferences" wrapper); this implementation expects
 * either a flat map or a map under "preferences".
 */
public final class ClasspathJsonRecommendationPreferenceSource implements ActionRecommendationPreferenceSource {

    private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {};

    private final Map<String, List<String>> preferredByContext;
    private final String sourceId;

    /**
     * @param classpathResource e.g. "policy/recommendation-preferences.json"
     * @param objectMapper      for JSON parsing
     */
    public ClasspathJsonRecommendationPreferenceSource(String classpathResource, ObjectMapper objectMapper) throws IOException {
        this.sourceId = "classpath-json:" + (classpathResource != null ? classpathResource : "");
        Map<String, List<String>> loaded = loadFromClasspath(classpathResource, objectMapper);
        this.preferredByContext = loaded != null ? Map.copyOf(loaded) : Map.of();
    }

    /**
     * Pre-loaded map (e.g. from tests). sourceId for trace.
     */
    public ClasspathJsonRecommendationPreferenceSource(Map<String, List<String>> preferredByContext, String sourceId) {
        this.preferredByContext = preferredByContext != null ? Map.copyOf(preferredByContext) : Map.of();
        this.sourceId = sourceId != null ? sourceId : "classpath-json:in-memory";
    }

    public String sourceId() {
        return sourceId;
    }

    @Override
    public List<String> getPreferredActionKeys(PromptCategory category, ActionIntent intent) {
        if (category == null || intent == null) return Collections.emptyList();
        String key = contextKey(category, intent);
        List<String> list = preferredByContext.get(key);
        return list != null ? List.copyOf(list) : Collections.emptyList();
    }

    private static String contextKey(PromptCategory category, ActionIntent intent) {
        return Objects.requireNonNull(category).name() + "+" + Objects.requireNonNull(intent).name();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> loadFromClasspath(String resource, ObjectMapper mapper) throws IOException {
        if (resource == null || resource.isBlank()) return Map.of();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = ClasspathJsonRecommendationPreferenceSource.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(resource)) {
            if (in == null) return Map.of();
            Map<String, Object> raw = mapper.readValue(in, MAP_REF);
            if (raw == null) return Map.of();
            Object prefs = raw.get("preferences");
            if (prefs instanceof Map) {
                return normalizeStringListMap((Map<?, ?>) prefs);
            }
            return normalizeStringListMap(raw);
        }
    }

    private static Map<String, List<String>> normalizeStringListMap(Map<?, ?> map) {
        if (map == null) return Map.of();
        Map<String, List<String>> out = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!(e.getKey() instanceof String)) continue;
            String key = (String) e.getKey();
            List<String> list = toListOfStrings(e.getValue());
            if (!list.isEmpty()) out.put(key, List.copyOf(list));
        }
        return Map.copyOf(out);
    }

    private static List<String> toListOfStrings(Object value) {
        if (value == null) return List.of();
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(s -> !s.isBlank())
                    .map(String::trim)
                    .toList();
        }
        return List.of(value.toString().trim());
    }
}
