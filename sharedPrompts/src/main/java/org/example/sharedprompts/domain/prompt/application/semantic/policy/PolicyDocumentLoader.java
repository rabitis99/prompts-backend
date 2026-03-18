package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import java.util.Map;

/**
 * Loads raw policy document from a source (classpath, file, string).
 * Does not parse or validate; returns raw structure (e.g. map) for parser.
 */
public interface PolicyDocumentLoader {

    /**
     * Load raw document from classpath resource.
     *
     * @param classpathResource e.g. "policy/recommendation-preferences.json"
     * @return raw map (e.g. from JSON/YAML) or empty map if not found
     */
    Map<String, Object> loadFromClasspath(String classpathResource);

    /**
     * Load raw document from string content (e.g. JSON/YAML string).
     */
    Map<String, Object> loadFromString(String content);
}
