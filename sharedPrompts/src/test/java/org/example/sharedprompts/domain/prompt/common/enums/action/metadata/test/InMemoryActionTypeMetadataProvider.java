package org.example.sharedprompts.domain.prompt.common.enums.action.metadata.test;

import org.example.sharedprompts.domain.prompt.common.enums.action.metadata.ActionTypeMetadataProvider;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Test-only in-memory implementation of {@link ActionTypeMetadataProvider}.
 * Used by tests that need a swappable provider without classpath properties.
 * Lives in {@code ...metadata.test} so production package {@code ...action.metadata} stays clean.
 */
public final class InMemoryActionTypeMetadataProvider implements ActionTypeMetadataProvider {

    private final Map<String, OutputBehaviorType> keyToOutputBehavior = new HashMap<>();
    private final Map<String, TaskDomain> keyToTaskDomain = new HashMap<>();

    public InMemoryActionTypeMetadataProvider putOutputBehavior(String key, OutputBehaviorType value) {
        String normalized = key == null ? null : key.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("key must not be null or blank");
        }
        keyToOutputBehavior.put(normalized, value);
        return this;
    }

    public InMemoryActionTypeMetadataProvider putTaskDomain(String key, TaskDomain value) {
        String normalized = key == null ? null : key.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("key must not be null or blank");
        }
        keyToTaskDomain.put(normalized, value);
        return this;
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
}
