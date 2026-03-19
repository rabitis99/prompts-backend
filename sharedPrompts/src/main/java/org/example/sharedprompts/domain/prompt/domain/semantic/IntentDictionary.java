package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 공식 Intent 사전: 각 {@link ActionIntent}의 정식 의미와 해석 기본값을 제공한다.
 * <p>
 * 실제 데이터(definition/default) 소유는 {@link IntentDefinitionDataSource}로 분리하고,
 * 이 클래스는 “인덱싱/검증/조회”에만 집중한다.
 */
public final class IntentDictionary {

    private static final Map<ActionIntent, IntentDefinition> DEFINITIONS;

    /**
     * Intent별 해석 기본값(objective, output needs, response shape).
     * 의미 해석의 기준 소스.
     */
    private static final Map<ActionIntent, IntentResolutionDefaults> RESOLUTION_DEFAULTS;

    static {
        Map<ActionIntent, IntentDefinition> defs = new HashMap<>();
        for (IntentDefinitionDataSource.IntentDefinitionEntry e : IntentDefinitionDataSource.definitionsEntries()) {
            defs.put(e.intent(), e.definition());
        }

        Map<ActionIntent, IntentResolutionDefaults> res = new HashMap<>();
        for (IntentDefinitionDataSource.IntentResolutionDefaultEntry e : IntentDefinitionDataSource.resolutionDefaultEntries()) {
            res.put(e.intent(), e.defaults());
        }

        // fail-fast: “등록 결과” 기준으로 completeness를 강제한다.
        EnumSet<ActionIntent> allIntents = EnumSet.allOf(ActionIntent.class);
        if (!defs.keySet().containsAll(allIntents) || !res.keySet().containsAll(allIntents)) {
            EnumSet<ActionIntent> missingDefinitions = EnumSet.copyOf(allIntents);
            missingDefinitions.removeAll(defs.keySet());
            EnumSet<ActionIntent> missingDefaults = EnumSet.copyOf(allIntents);
            missingDefaults.removeAll(res.keySet());
            throw new IllegalStateException(
                    "IntentDictionary is incomplete. missingDefinitions=" + missingDefinitions
                            + ", missingDefaults=" + missingDefaults
            );
        }

        DEFINITIONS = Collections.unmodifiableMap(new HashMap<>(defs));
        RESOLUTION_DEFAULTS = Collections.unmodifiableMap(new HashMap<>(res));
    }

    private IntentDictionary() {}

    /**
     * Resolution defaults for the given intent.
     * semantic resolution flow에서 사용한다.
     */
    public static IntentResolutionDefaults getResolutionDefaults(ActionIntent intent) {
        IntentResolutionDefaults d = RESOLUTION_DEFAULTS.get(intent);
        if (d == null) {
            throw new IllegalArgumentException("No resolution defaults for: " + intent);
        }
        return d;
    }

    public static Optional<IntentDefinition> get(ActionIntent intent) {
        return Optional.ofNullable(DEFINITIONS.get(intent));
    }

    public static IntentDefinition getOrThrow(ActionIntent intent) {
        IntentDefinition d = DEFINITIONS.get(intent);
        if (d == null) {
            throw new IllegalArgumentException("No IntentDefinition for: " + intent);
        }
        return d;
    }
}

