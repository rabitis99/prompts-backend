package org.example.sharedprompts.domain.prompt.infrastructure.policy.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loads raw policy document from classpath or string (JSON or YAML).
 * No parsing into schema or validation.
 */
public final class DefaultPolicyDocumentLoader implements PolicyDocumentLoader {

    private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final Yaml yaml;

    public DefaultPolicyDocumentLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.yaml = new Yaml();
    }

    @Override
    public Map<String, Object> loadFromClasspath(String classpathResource) {
        if (classpathResource == null || classpathResource.isBlank()) {
            return Map.of();
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = getClass().getClassLoader();
        try (InputStream in = cl.getResourceAsStream(classpathResource)) {
            if (in == null) return Map.of();
            if (classpathResource.endsWith(".yaml") || classpathResource.endsWith(".yml")) {
                return loadYamlStream(in);
            }
            return objectMapper.readValue(in, MAP_REF);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load policy from classpath: " + classpathResource, e);
        }
    }

    @Override
    public Map<String, Object> loadFromString(String content) {
        if (content == null || content.isBlank()) return Map.of();
        String trimmed = content.trim();
        try {
            if (trimmed.startsWith("{")) {
                return objectMapper.readValue(trimmed, MAP_REF);
            }
            return yaml.load(trimmed) instanceof Map ? (Map<String, Object>) yaml.load(trimmed) : Map.of();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse policy content", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYamlStream(InputStream in) {
        Object raw = yaml.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        if (raw == null) return Map.of();
        return raw instanceof Map ? (Map<String, Object>) raw : Map.of();
    }
}
