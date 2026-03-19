package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

import java.util.Optional;

/**
 * Objective inference from action name (heuristic fallback).
 *
 * <p>Registry owns overlay + policy-source lookup; this policy owns "keyword mapping" and
 * action-name matching rules so it can be swapped/extended independently.
 */
public interface ObjectiveHeuristicInferencePolicy {

    /**
     * Infer an objective from the action type name (heuristic).
     *
     * @param actionType action type (null-safe)
     * @return matched objective; empty when no heuristic matches
     */
    Optional<PromptObjective> inferByActionName(ActionTypeInterface actionType);
}

