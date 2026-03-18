package org.example.sharedprompts.domain.prompt.common.enums.action.metadata;

import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Map;
import java.util.Optional;

/**
 * Runtime metadata for ActionType by stable key.
 * Definition model (enum/identity) is separate; this contract supplies policy/metadata at runtime.
 */
public interface ActionTypeMetadataProvider {

    /**
     * Output behavior for the given stable key.
     *
     * @param key stable key (e.g. actionType.key())
     * @return present if metadata exists for the key
     */
    Optional<OutputBehaviorType> getOutputBehavior(String key);

    /**
     * Task domain for the given stable key.
     *
     * @param key stable key
     * @return present if metadata exists for the key
     */
    Optional<TaskDomain> getTaskDomain(String key);

    /**
     * Optional bulk access for callers that need full coverage (typically tests or bootstrap utilities).
     * Implementations may override to provide a complete mapping; default is empty.
     */
    default Map<String, OutputBehaviorType> getKeyToOutputBehaviorMap() {
        return Map.of();
    }

    /**
     * Optional bulk access for callers that need full coverage (typically tests or bootstrap utilities).
     * Implementations may override to provide a complete mapping; default is empty.
     */
    default Map<String, TaskDomain> getKeyToTaskDomainMap() {
        return Map.of();
    }
}
