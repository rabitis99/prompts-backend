package org.example.sharedprompts.domain.prompt.common.enums.action.canonical;

import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionOutputBehaviorRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.DefaultActionOutputBehaviorRegistry;
import org.example.sharedprompts.domain.prompt.infrastructure.metadata.ClasspathActionTypeMetadataProvider;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileSeedSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Governance guardrails for action taxonomy: single-leaf groups, required metadata,
 * duplicate keys, group-first profiles, deprecated traceability. See docs/action-taxonomy-architecture.md.
 */
@DisplayName("Action taxonomy guardrails")
class ActionTaxonomyGuardrailTest {

    @Test
    @DisplayName("no duplicate stable keys across all ActionTypes")
    void noDuplicateStableKeys() {
        Set<String> keys = new HashSet<>();
        for (Class<? extends Enum<?>> enumClass : DeserializerEnumTestUtils.getActionTypeEnums()) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) continue;
            for (Enum<?> c : enumClass.getEnumConstants()) {
                ActionTypeInterface action = (ActionTypeInterface) c;
                String key = action.key();
                assertThat(keys).as("Duplicate stable key: " + key).doesNotContain(key);
                keys.add(key);
            }
        }
    }

    @Test
    @DisplayName("single-leaf ActionGroups are known and intentional")
    void singleLeafGroupsAreDocumented() {
        Map<ActionGroup, List<ActionTypeInterface>> groupToActions = new HashMap<>();
        for (Class<? extends Enum<?>> enumClass : DeserializerEnumTestUtils.getActionTypeEnums()) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) continue;
            for (Enum<?> c : enumClass.getEnumConstants()) {
                ActionTypeInterface action = (ActionTypeInterface) c;
                ActionGroup group = action.getActionGroup();
                if (group == null) continue;
                groupToActions.computeIfAbsent(group, k -> new ArrayList<>()).add(action);
            }
        }
        List<ActionGroup> singleLeaf = groupToActions.entrySet().stream()
                .filter(e -> e.getValue().size() == 1)
                .map(Map.Entry::getKey)
                .sorted(Enum::compareTo)
                .toList();
        Set<ActionGroup> documentedSingleLeafGroups = Set.of(
                ActionGroup.DEBUGGING,
                ActionGroup.TRANSLATION,
                ActionGroup.INTERVIEW_PREPARATION,
                ActionGroup.CLOUD_NETWORKING,
                ActionGroup.CLOUD_SECURITY,
                ActionGroup.CLOUD_COST_OPTIMIZATION,
                ActionGroup.PERFORMANCE_OPTIMIZATION,
                ActionGroup.FINANCIAL_ANALYSIS,
                ActionGroup.COMPLAINT_RESPONSE
        );
        assertThat(new HashSet<>(singleLeaf))
                .as("Single-leaf groups must exactly match the documented intentional set")
                .isEqualTo(documentedSingleLeafGroups);
    }

    @Test
    @DisplayName("every ActionType has non-null ActionGroup and required metadata (identity + registry)")
    void everyActionHasRequiredMetadata() {
        ClasspathActionTypeMetadataProvider provider = new ClasspathActionTypeMetadataProvider();
        ActionOutputBehaviorRegistry outputBehaviorRegistry = new DefaultActionOutputBehaviorRegistry(
                provider.getKeyToOutputBehaviorMap());
        for (Class<? extends Enum<?>> enumClass : DeserializerEnumTestUtils.getActionTypeEnums()) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) continue;
            for (Enum<?> c : enumClass.getEnumConstants()) {
                ActionTypeInterface action = (ActionTypeInterface) c;
                assertThat(action.getActionGroup())
                        .as(enumClass.getSimpleName() + "." + c.name() + " must have action group")
                        .isNotNull();
                assertThat(action.key()).as(enumClass.getSimpleName() + "." + c.name() + " must have key").isNotBlank();
                assertThat(action.getDisplayNameKo()).as("displayNameKo").isNotBlank();
                assertThat(action.getDisplayNameEn()).as("displayNameEn").isNotBlank();
                assertThat(action.getDisplayNameJa()).as("displayNameJa").isNotBlank();
                assertThat(outputBehaviorRegistry.getOutputBehavior(action))
                        .as("outputBehavior from registry for " + action.key())
                        .isPresent();
            }
        }
    }

    @Test
    @DisplayName("market/customer analysis actions map to DATA_ANALYSIS after taxonomy merge")
    void marketCustomerAnalysisMapsToDataAnalysis() {
        var registry = new org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry(
                DeserializerEnumTestUtils.getActionTypeEnums());
        var canonical = new DefaultCanonicalActionRegistry(registry);
        ActionTypeInterface marketResearch = registry.getByStableKey("ACTION.MARKETING.MARKET_RESEARCH");
        ActionTypeInterface customerAnalysis = registry.getByStableKey("ACTION.MARKETING.CUSTOMER_ANALYSIS");
        assertThat(marketResearch).isNotNull();
        assertThat(customerAnalysis).isNotNull();
        assertThat(canonical.toCanonical(marketResearch)).contains(ActionGroup.DATA_ANALYSIS);
        assertThat(canonical.toCanonical(customerAnalysis)).contains(ActionGroup.DATA_ANALYSIS);
    }

    @Test
    @DisplayName("profile compatibility is group-first: categories with actions have non-empty action groups per intent")
    void profileCompatibilityIsGroupFirst() {
        ActionTypeRegistry actionTypeRegistry = new ActionTypeRegistry(DeserializerEnumTestUtils.getActionTypeEnums());
        DefaultCanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(actionTypeRegistry);
        CategorySemanticProfileRegistry profileRegistry =
                new DefaultCategorySemanticProfileRegistry(
                        canonical,
                        null,
                        new DefaultCategorySemanticProfileSeedSource()
                );
        List<PromptCategory> categoriesWithProfiles = List.of(
                PromptCategory.DESIGN, PromptCategory.DEVELOPMENT, PromptCategory.WRITING,
                PromptCategory.RESEARCH, PromptCategory.BUSINESS, PromptCategory.MARKETING,
                PromptCategory.CUSTOMER_SUPPORT, PromptCategory.DATA_ANALYSIS, PromptCategory.PRODUCTIVITY
        );
        for (PromptCategory category : categoriesWithProfiles) {
            var profileOpt = profileRegistry.getProfile(category);
            assertThat(profileOpt)
                    .as(category + " must have a semantic profile")
                    .isPresent();
            var profile = profileOpt.orElseThrow();
            for (ActionIntent intent : profile.getAllowedIntents()) {
                List<ActionTypeInterface> actions = profile.getCompatibleActionsForIntent(intent);
                if (!actions.isEmpty()) {
                    List<ActionGroup> groups = profile.getCompatibleActionGroupsForIntent(intent);
                    assertThat(groups).as(category + "+" + intent + " has actions so must have groups").isNotEmpty();
                }
            }
        }
    }

    @Test
    @DisplayName("deprecated action keys still resolve and map to expected group (traceability)")
    void deprecatedActionsRemainTraceable() {
        ActionTypeRegistry registry = new ActionTypeRegistry(DeserializerEnumTestUtils.getActionTypeEnums());
        DefaultCanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(registry);
        // Deprecated channel variants: still resolve, same group as non-deprecated
        ActionTypeInterface textMessage = registry.getByStableKey("ACTION.WRITING.TEXT_MESSAGE");
        ActionTypeInterface whatsapp = registry.getByStableKey("ACTION.WRITING.WHATSAPP_MESSAGE");
        assertThat(textMessage).isNotNull();
        assertThat(whatsapp).isNotNull();
        assertThat(canonical.toCanonical(textMessage)).contains(ActionGroup.MESSAGE_COMPOSITION);
        assertThat(canonical.toCanonical(whatsapp)).contains(ActionGroup.MESSAGE_COMPOSITION);
        // Deprecated duplicate of CreativeActionType.CREATIVE_WRITING
        ActionTypeInterface creativeWritingGen = registry.getByStableKey("ACTION.WRITING.CREATIVE_WRITING_GEN");
        assertThat(creativeWritingGen).isNotNull();
        assertThat(canonical.toCanonical(creativeWritingGen)).contains(ActionGroup.CREATIVE_WRITING);
    }
}
