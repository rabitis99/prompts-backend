package org.example.sharedprompts.domain.prompt.domain.semantic.seed;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.marketing.MarketingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.marketing.MarketingRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeed;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeedDefinition;
import org.example.sharedprompts.domain.prompt.domain.semantic.FallbackCandidate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MarketingSemanticProfileSeed {

    public static final CategorySemanticProfileSeedDefinition DEFINITION =
            new CategorySemanticProfileSeedDefinition(PromptCategory.MARKETING, MarketingSemanticProfileSeed::build);

    private MarketingSemanticProfileSeed() {
    }

    private static CategorySemanticProfileSeed build() {
        Map<ActionIntent, List<RoleTypeInterface>> roles = new HashMap<>();
        roles.put(ActionIntent.GENERATE, List.of(MarketingRoleType.DIGITAL_MARKETER, MarketingRoleType.MARKETING_STRATEGIST));
        roles.put(ActionIntent.PLAN, List.of(MarketingRoleType.MARKETING_STRATEGIST, MarketingRoleType.BRAND_SPECIALIST));

        Map<ActionIntent, List<ActionTypeInterface>> actions = new HashMap<>();
        actions.put(ActionIntent.GENERATE, List.of(MarketingActionType.CONTENT_MARKETING, MarketingActionType.AD_CAMPAIGN));
        actions.put(ActionIntent.PLAN, List.of(MarketingActionType.MARKETING_STRATEGY, MarketingActionType.SOCIAL_MEDIA_STRATEGY));

        Set<ActionIntent> allowed = Set.of(ActionIntent.GENERATE, ActionIntent.PLAN, ActionIntent.ANALYZE, ActionIntent.REWRITE);
        return new CategorySemanticProfileSeed(
                PromptCategory.MARKETING, TaskDomain.PRACTICAL, allowed, Map.of(), roles, actions,
                null, null, ActionIntent.GENERATE,
                List.of(new FallbackCandidate(ActionIntent.GENERATE, "Marketing default: generate content or campaigns; use PLAN/ANALYZE/REWRITE as needed.")));
    }
}
