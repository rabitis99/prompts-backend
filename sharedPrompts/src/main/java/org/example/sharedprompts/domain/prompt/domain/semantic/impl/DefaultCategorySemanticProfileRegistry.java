package org.example.sharedprompts.domain.prompt.domain.semantic.impl;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeed;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeedSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.CompatibilityPolicySource;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Assembles category semantic profiles from a {@link CategorySemanticProfileSeedSource}.
 * Does not own seed data; builds profiles from source + {@link CompatibilityPolicySource} override.
 *
 * <p><b>EXTRACTION</b> does NOT use {@link CategorySemanticProfile}; do not add it to profile categories.</p>
 * <p>Legacy categories resolve via {@link PromptCategory#canonical()} to the same profile as their canonical.</p>
 * <p>Wired via config bean. Optional {@link CompatibilityPolicySource}: when it returns non-empty group keys
 * for (category, intent), those override action-derived groups.</p>
 */
public class DefaultCategorySemanticProfileRegistry implements CategorySemanticProfileRegistry {

    private final Map<PromptCategory, CategorySemanticProfile> profiles;
    private final CanonicalActionRegistry canonicalActionRegistry;
    private final CompatibilityPolicySource compatibilityPolicySource;

    /** Convenience overload with no compatibility source. */
    public DefaultCategorySemanticProfileRegistry(CanonicalActionRegistry canonicalActionRegistry,
                                                 CategorySemanticProfileSeedSource seedSource) {
        this(canonicalActionRegistry, null, seedSource);
    }

    /** Full constructor: assembles profiles from seed source; compatibility source overrides group keys when non-empty. */
    public DefaultCategorySemanticProfileRegistry(CanonicalActionRegistry canonicalActionRegistry,
                                                   CompatibilityPolicySource compatibilityPolicySource,
                                                   CategorySemanticProfileSeedSource seedSource) {
        this.canonicalActionRegistry = Objects.requireNonNull(canonicalActionRegistry, "canonicalActionRegistry");
        this.compatibilityPolicySource = compatibilityPolicySource;
        Objects.requireNonNull(seedSource, "seedSource");
        Map<PromptCategory, CategorySemanticProfile> map = new HashMap<>();
        for (PromptCategory category : seedSource.profileCategoriesForRegistry()) {
            CategorySemanticProfileSeed seed = seedSource.requireSeed(category);
            map.put(category, buildProfile(seed));
        }
        this.profiles = Collections.unmodifiableMap(map);
    }

    @Override
    public Optional<CategorySemanticProfile> getProfile(PromptCategory category) {
        if (category == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(profiles.get(category.canonical()));
    }

    private CategorySemanticProfile buildProfile(CategorySemanticProfileSeed seed) {
        GroupMapAndActions prepared = prepare(seed.category(), seed.actionsByIntent());
        return new DefaultCategorySemanticProfile(
                seed.category(),
                seed.taskDomain(),
                seed.allowedIntents(),
                seed.intentFitLevels(),
                seed.rolesByIntent(),
                prepared.actions(),
                seed.discouragedTonesByIntent(),
                seed.discouragedStylesByIntent(),
                seed.fallbackIntent(),
                seed.fallbackCandidates(),
                prepared.groupMap());
    }

    /**
     * Strict: every action listed under an intent in a category seed must resolve to an {@link ActionGroup};
     * silent drops are not allowed (misconfiguration must fail fast at registry construction).
     */
    private Map<ActionIntent, List<ActionGroup>> toActionGroupMap(
            PromptCategory category, Map<ActionIntent, List<ActionTypeInterface>> actions) {
        if (actions == null || actions.isEmpty()) return Map.of();
        Map<ActionIntent, List<ActionGroup>> out = new HashMap<>();
        for (Map.Entry<ActionIntent, List<ActionTypeInterface>> e : actions.entrySet()) {
            ActionIntent intent = e.getKey();
            List<ActionTypeInterface> actionList = e.getValue();
            if (actionList == null || actionList.isEmpty()) {
                throw new IllegalArgumentException(
                        "Category semantic profile seed contains empty action list for category="
                                + category.name()
                                + ", intent="
                                + intent.name());
            }
            List<ActionGroup> groups = actionList.stream()
                    .map(a -> requireActionGroupForCategorySeed(category, intent, a))
                    .distinct()
                    .toList();
            assertIntentHasResolvedActionGroups(category, intent, groups);
            out.put(intent, groups);
        }
        return out;
    }

    /**
     * Package-private for tests: same invariant as {@link #toActionGroupMap(PromptCategory, Map)} after mapping.
     */
    static void assertIntentHasResolvedActionGroups(
            PromptCategory category, ActionIntent intent, List<ActionGroup> groups) {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(groups, "groups");
        if (groups.isEmpty()) {
            throw new IllegalArgumentException(
                    "No ActionGroup resolved for category="
                            + category.name()
                            + ", intent="
                            + intent.name());
        }
    }

    private ActionGroup requireActionGroupForCategorySeed(
            PromptCategory category, ActionIntent intent, ActionTypeInterface actionType) {
        try {
            return canonicalActionRegistry.requireActionGroup(actionType);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Category semantic profile seed assembly: cannot resolve ActionGroup for category="
                            + category.name()
                            + ", intent="
                            + intent.name()
                            + ", actionStableKey="
                            + actionType.key(),
                    ex);
        }
    }

    /**
     * When compatibilityPolicySource returns non-empty for (category, intent), those group keys are used;
     * otherwise action-derived groups from the seed are used.
     */
    private GroupMapAndActions prepare(PromptCategory category, Map<ActionIntent, List<ActionTypeInterface>> actions) {
        Map<ActionIntent, List<ActionGroup>> fromActions = toActionGroupMap(category, actions);
        if (compatibilityPolicySource == null) {
            return new GroupMapAndActions(fromActions, actions != null ? actions : Map.of());
        }
        Map<ActionIntent, List<ActionGroup>> groupMap = new HashMap<>();
        Set<ActionIntent> intents = actions != null ? actions.keySet() : Set.of();
        for (ActionIntent intent : intents) {
            List<String> keys = compatibilityPolicySource.getCompatibleGroupKeys(category, intent);
            if (!keys.isEmpty()) {
                List<ActionGroup> fromSource = requireResolvedCompatibilityActionGroups(category, intent, keys);
                groupMap.put(intent, fromSource);
                continue;
            }
            if (fromActions.containsKey(intent)) {
                groupMap.put(intent, fromActions.get(intent));
            }
        }
        return new GroupMapAndActions(groupMap, actions != null ? actions : Map.of());
    }

    /**
     * Resolves every compatibility key strictly; invalid or blank entries are configuration errors, not absences.
     */
    private static List<ActionGroup> requireResolvedCompatibilityActionGroups(
            PromptCategory category,
            ActionIntent intent,
            List<String> keys) {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(keys, "keys");
        return keys.stream()
                .map(k -> requireActionGroupForCompatibility(category, intent, k))
                .distinct()
                .toList();
    }

    private static ActionGroup requireActionGroupForCompatibility(
            PromptCategory category,
            ActionIntent intent,
            String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Blank or null ActionGroup key in compatibility policy for category "
                            + category.name()
                            + ", intent "
                            + intent.name());
        }
        String trimmed = rawKey.trim();
        try {
            return ActionGroup.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid ActionGroup key '"
                            + trimmed
                            + "' for category "
                            + category.name()
                            + ", intent "
                            + intent.name(),
                    e);
        }
    }

    private record GroupMapAndActions(Map<ActionIntent, List<ActionGroup>> groupMap,
                                      Map<ActionIntent, List<ActionTypeInterface>> actions) {}
}
