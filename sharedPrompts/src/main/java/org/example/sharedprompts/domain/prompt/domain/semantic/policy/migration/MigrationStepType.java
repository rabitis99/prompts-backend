package org.example.sharedprompts.domain.prompt.domain.semantic.policy.migration;

/**
 * Type of a single migration step between policy versions.
 * Used to record evolution path (rename, split, merge, remove).
 */
public enum MigrationStepType {

    RENAME_RULE,
    SPLIT_RULE,
    MERGE_RULE,
    REMOVE_RULE
}
