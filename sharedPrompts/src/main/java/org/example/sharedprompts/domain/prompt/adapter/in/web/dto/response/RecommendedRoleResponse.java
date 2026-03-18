package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Role recommendation item in API response.
 * <p>Contract: {@code roleKey} is the stable identifier; {@code roleDisplayName} is convenience only.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record RecommendedRoleResponse(

        /** Stable identifier for API/UI/storage. External contract. */
        @JsonProperty("role_key")
        String roleKey,

        /** Display label; not part of contract. */
        @JsonProperty("role_display_name")
        String roleDisplayName,

        /** Optional: why this role was recommended (explainability). */
        @JsonProperty("reason")
        String reason,

        /** Optional: source of recommendation (e.g. policy, fallback). */
        @JsonProperty("source")
        String source,

        /** Optional: ordering basis (e.g. preference, stable key order). */
        @JsonProperty("ordering_basis")
        String orderingBasis
) {
    public static RecommendedRoleResponse of(String roleKey, String roleDisplayName) {
        return new RecommendedRoleResponse(roleKey, roleDisplayName, null, null, null);
    }

    public static RecommendedRoleResponse withExplanation(String roleKey, String roleDisplayName,
                                                          String reason, String source, String orderingBasis) {
        return new RecommendedRoleResponse(roleKey, roleDisplayName, reason, source, orderingBasis);
    }
}
