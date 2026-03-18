package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.DeserializerEnumTestUtils;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.DefaultCanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveMappingRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.DefaultObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.ObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 3차 구조 리팩터링 검증: canonical definition-first, profile no getAll(), resolution key-based.
 */
@DisplayName("Semantic structure phase 3: definition vs runtime policy")
class SemanticStructurePhase3Test {

    private static final List<Class<? extends Enum<?>>> CATALOG = DeserializerEnumTestUtils.getActionTypeEnums();

    @Test
    @DisplayName("Profile compatible groups do not depend on registry getAll() order")
    void profileDoesNotDependOnRegistryOrder() {
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(new ActionTypeRegistry(CATALOG));
        List<Class<? extends Enum<?>>> reversed = new ArrayList<>(CATALOG);
        Collections.reverse(reversed);
        ActionTypeRegistry registryReversed = new ActionTypeRegistry(reversed);

        DefaultCategorySemanticProfileRegistry profileOrderA = new DefaultCategorySemanticProfileRegistry(canonical);
        DefaultCategorySemanticProfileRegistry profileOrderB = new DefaultCategorySemanticProfileRegistry(
                new DefaultCanonicalActionRegistry(registryReversed));

        for (PromptCategory category : List.of(PromptCategory.WRITING, PromptCategory.DEVELOPMENT, PromptCategory.RESEARCH)) {
            var profA = profileOrderA.getProfile(category);
            var profB = profileOrderB.getProfile(category);
            if (profA.isEmpty() || profB.isEmpty()) continue;
            for (ActionIntent intent : profA.get().getAllowedIntents()) {
                List<ActionGroup> groupsA = profA.get().getCompatibleActionGroupsForIntent(intent);
                List<ActionGroup> groupsB = profB.get().getCompatibleActionGroupsForIntent(intent);
                assertThat(groupsB)
                        .as("Profile compatible groups must be identical regardless of registry order: " + category + "+" + intent)
                        .containsExactlyInAnyOrder(groupsA.toArray(ActionGroup[]::new));
            }
        }
    }

    @Test
    @DisplayName("Objective mapping works by stable key without concrete enum reference")
    void objectiveMappingByStableKey() {
        ActionTypeRegistry actionTypeRegistry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(actionTypeRegistry);
        ObjectivePolicySource source = new DefaultObjectivePolicySource(Map.of());
        ObjectiveMappingRegistry mapping = new ObjectiveMappingRegistry(canonical, source);

        mapping.putByStableKey("ACTION.CODING.CODE_REVIEW", PromptObjective.REASONING);
        mapping.putByStableKey("ACTION.WRITING.TRANSLATION", PromptObjective.FACTUAL);

        ActionTypeInterface codeReview = actionTypeRegistry.getByStableKey("ACTION.CODING.CODE_REVIEW");
        ActionTypeInterface translation = actionTypeRegistry.getByStableKey("ACTION.WRITING.TRANSLATION");
        assertThat(codeReview).isNotNull();
        assertThat(translation).isNotNull();
        assertThat(mapping.findByActionType(codeReview)).contains(PromptObjective.REASONING);
        assertThat(mapping.findByActionType(translation)).contains(PromptObjective.FACTUAL);
    }

    @Test
    @DisplayName("New action key can be added to policy without changing config class")
    void extensibilityByStableKeyOnly() {
        ActionTypeRegistry actionTypeRegistry = new ActionTypeRegistry(CATALOG);
        CanonicalActionRegistry canonical = new DefaultCanonicalActionRegistry(actionTypeRegistry);
        ObjectivePolicySource source = new DefaultObjectivePolicySource(Map.of());
        ObjectiveMappingRegistry mapping = new ObjectiveMappingRegistry(canonical, source);

        String someExistingKey = "ACTION.CODING.DEBUGGING";
        mapping.putByStableKey(someExistingKey, PromptObjective.REASONING);
        assertThat(mapping.findByActionType(actionTypeRegistry.getByStableKey(someExistingKey)))
                .contains(PromptObjective.REASONING);
    }
}
