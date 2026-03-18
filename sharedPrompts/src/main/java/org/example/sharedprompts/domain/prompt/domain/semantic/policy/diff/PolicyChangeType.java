package org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff;

/**
 * Classification of a policy change for impact and review workflow.
 * SAFE: no behavioral change to recommendations.
 * BEHAVIOR_CHANGE: order or preference change that can alter recommendation output.
 * BREAKING: removal or compatibility change that can invalidate prior behavior.
 */
public enum PolicyChangeType {

    /** Preference add, metadata change; recommendation output shape unchanged. */
    SAFE,

    /** Preference reorder, role ordering change; same options, different order. */
    BEHAVIOR_CHANGE,

    /** Compatibility removal, action/role removal; may break prior recommendations. */
    BREAKING
}
