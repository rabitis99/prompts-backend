package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeSerializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeSerializer;

import java.util.List;
import java.util.Map;

/**
 * 프롬프트 축 추천 결과 응답 DTO
 */
public record RecommendPromptResponse(

        @JsonProperty("request_mode")
        RequestMode requestMode,

        PromptCategory category,

        @JsonProperty("recommended_intent")
        ActionIntent recommendedIntent,

        @JsonProperty("intent_candidates")
        List<ActionIntent> intentCandidates,

        @JsonProperty("recommended_role")
        @JsonSerialize(using = RoleTypeSerializer.class)
        RoleTypeInterface recommendedRole,

        @JsonProperty("role_candidates")
        @JsonSerialize(contentUsing = RoleTypeSerializer.class)
        List<RoleTypeInterface> roleCandidates,

        @JsonProperty("recommended_action")
        @JsonSerialize(using = ActionTypeSerializer.class)
        ActionTypeInterface recommendedAction,

        @JsonProperty("action_candidates")
        @JsonSerialize(contentUsing = ActionTypeSerializer.class)
        List<ActionTypeInterface> actionCandidates,

        @JsonProperty("recommended_tone")
        ToneType recommendedTone,

        @JsonProperty("recommended_style")
        StyleType recommendedStyle,

        @JsonProperty("axis_sources")
        Map<String, String> axisSources,

        @JsonProperty("recommendation_hints")
        List<String> recommendationHints,

        @JsonProperty("validation_warnings")
        List<String> validationWarnings,

        @JsonProperty("fallback_applied")
        List<String> fallbackApplied,

        @JsonProperty("default_selection")
        String defaultSelection
) {
}