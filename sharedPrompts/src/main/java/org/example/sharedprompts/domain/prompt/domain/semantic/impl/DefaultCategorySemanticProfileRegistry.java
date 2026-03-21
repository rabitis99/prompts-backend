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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(DefaultCategorySemanticProfileRegistry.class);

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

    private Map<ActionIntent, List<ActionGroup>> toActionGroupMap(Map<ActionIntent, List<ActionTypeInterface>> actions) {
        if (actions == null || actions.isEmpty()) return Map.of();
        Map<ActionIntent, List<ActionGroup>> out = new HashMap<>();
        for (Map.Entry<ActionIntent, List<ActionTypeInterface>> e : actions.entrySet()) {
            List<ActionGroup> groups = e.getValue().stream()
                    .map(canonicalActionRegistry::toCanonical)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .distinct()
                    .toList();
            if (!groups.isEmpty()) out.put(e.getKey(), groups);
        }
        return out;
    }

    /**
     * When compatibilityPolicySource returns non-empty for (category, intent), those group keys are used;
     * otherwise action-derived groups from the seed are used.
     */
    private GroupMapAndActions prepare(PromptCategory category, Map<ActionIntent, List<ActionTypeInterface>> actions) {
        Map<ActionIntent, List<ActionGroup>> fromActions = toActionGroupMap(actions);
        if (compatibilityPolicySource == null) {
            return new GroupMapAndActions(fromActions, actions != null ? actions : Map.of());
        }
        Map<ActionIntent, List<ActionGroup>> groupMap = new HashMap<>();
        Set<ActionIntent> intents = actions != null ? actions.keySet() : Set.of();
        for (ActionIntent intent : intents) {
            List<String> keys = compatibilityPolicySource.getCompatibleGroupKeys(category, intent);
            if (!keys.isEmpty()) {
                List<ActionGroup> fromSource = resolveGroupKeys(keys);
                if (!fromSource.isEmpty()) {
                    groupMap.put(intent, fromSource);
                    continue;
                }
            }
            if (fromActions.containsKey(intent)) {
                groupMap.put(intent, fromActions.get(intent));
            }
        }
        return new GroupMapAndActions(groupMap, actions != null ? actions : Map.of());
    }

    private List<ActionGroup> resolveGroupKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) return List.of();
        return keys.stream()
                .map(this::parseActionGroup)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .toList();
    }

    private Optional<ActionGroup> parseActionGroup(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        String trimmed = key.trim();
        try {
            return Optional.of(ActionGroup.valueOf(trimmed));
        } catch (IllegalArgumentException e) {
            log.warn(
                    "Ignoring invalid compatibility policy ActionGroup key '{}': {}",
                    trimmed,
                    e.toString());
            return Optional.empty();
        }
    }

    private record GroupMapAndActions(Map<ActionIntent, List<ActionGroup>> groupMap,
                                      Map<ActionIntent, List<ActionTypeInterface>> actions) {}
}
