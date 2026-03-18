package org.example.sharedprompts.domain.prompt.infrastructure.metadata;

import org.example.sharedprompts.domain.prompt.common.enums.action.metadata.ActionTypeMetadataProvider;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Loads ActionType metadata from classpath properties. Fail-fast when resources are missing.
 */
public final class ClasspathActionTypeMetadataProvider implements ActionTypeMetadataProvider {

    public static final String OUTPUT_BEHAVIOR_RESOURCE = "action-type-output-behavior.properties";
    public static final String DOMAIN_RESOURCE = "action-type-domain.properties";

    private final Map<String, OutputBehaviorType> keyToOutputBehavior;
    private final Map<String, TaskDomain> keyToTaskDomain;

    public ClasspathActionTypeMetadataProvider() {
        this(OUTPUT_BEHAVIOR_RESOURCE, DOMAIN_RESOURCE);
    }

    /**
     * For tests or custom paths. Fails if either resource is missing.
     */
    public ClasspathActionTypeMetadataProvider(String outputBehaviorResource, String domainResource) {
        this.keyToOutputBehavior = loadKeyToEnum(outputBehaviorResource, OutputBehaviorType.class);
        this.keyToTaskDomain = loadKeyToEnum(domainResource, TaskDomain.class);
    }

    @Override
    public Optional<OutputBehaviorType> getOutputBehavior(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(keyToOutputBehavior.get(key.trim()));
    }

    @Override
    public Optional<TaskDomain> getTaskDomain(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(keyToTaskDomain.get(key.trim()));
    }

    /** Exposes full map for bootstrap or tests that need a Map-based registry. */
    public Map<String, OutputBehaviorType> getKeyToOutputBehaviorMap() {
        return keyToOutputBehavior;
    }

    /** Exposes full map for bootstrap or tests that need a Map-based registry. */
    public Map<String, TaskDomain> getKeyToTaskDomainMap() {
        return keyToTaskDomain;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> Map<String, E> loadKeyToEnum(String resource, Class<E> enumClass) {
        ClassLoader cl = ClasspathActionTypeMetadataProvider.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing action metadata resource: " + resource + " (fail-fast)");
            }
            Properties props = new Properties();
            props.load(in);
            Map<String, E> result = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                if (value == null || value.isBlank()) continue;
                String trimmedKey = key.trim();
                String trimmedValue = value.trim();
                if (trimmedKey.isEmpty()) continue;
                try {
                    result.put(trimmedKey, (E) Enum.valueOf(enumClass, trimmedValue));
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException(
                            "Invalid " + enumClass.getSimpleName() + " mapping for key '" + trimmedKey + "' in " + resource,
                            e
                    );
                }
            }
            return Collections.unmodifiableMap(result);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load action metadata resource: " + resource, e);
        }
    }
}
