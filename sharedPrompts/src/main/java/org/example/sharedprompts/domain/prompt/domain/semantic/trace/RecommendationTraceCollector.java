package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.List;

/**
 * Collects structured trace during recommendation flow.
 * Service and policies record phase-wise; no free-form strings.
 * Builds RecommendationTrace at the end.
 */
public interface RecommendationTraceCollector {

    /** Record compatibility phase: allowed groups/actions and which source allowed them. */
    void recordCompatibility(List<String> allowedGroupNames, List<String> allowedActionKeys, PolicyApplicationTrace policyTrace);

    /** Record expansion phase: concrete action keys produced and which expander. */
    void recordExpansion(List<String> expandedActionKeys, PolicyApplicationTrace policyTrace);

    /** Record action ordering: policy applied, fallback flag, per-action rank source. */
    void recordActionOrdering(OrderingTrace orderingTrace);

    /** Set per-action traces (inclusion + optional ordering). Call after ordering. */
    void setActionTraces(List<ActionRecommendationTrace> actionTraces);

    /** Record role ordering (for recommended roles list). */
    void recordRoleOrdering(OrderingTrace orderingTrace);

    /** Set per-role traces. */
    void setRoleTraces(List<RoleRecommendationTrace> roleTraces);

    /** Set policy version/source/experiment for audit. Call before build(). */
    void setPolicyTraceInfo(PolicyTraceInfo policyTraceInfo);

    /** Build immutable RecommendationTrace. Call once after all phases. */
    RecommendationTrace build();
}
