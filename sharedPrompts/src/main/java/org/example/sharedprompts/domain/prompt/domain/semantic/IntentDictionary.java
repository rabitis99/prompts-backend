package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 공식 Intent 사전: 각 {@link ActionIntent}의 정식 의미와 해석 기본값을 제공한다.
 * <p>
 * Definition/default 데이터는 {@link IntentDefinitionDataSource}가 집계하고,
 * 운영 구성은 {@link IntentDefinitionProviderAssembly}가 조합한다.
 * 이 클래스는 인덱싱·completeness 검증·조회만 담당한다.
 */
public final class IntentDictionary {

    private final Map<ActionIntent, IntentDefinition> definitions;

    /**
     * Intent별 해석 기본값(objective, output needs, response shape).
     * 의미 해석의 기준 소스.
     */
    private final Map<ActionIntent, IntentResolutionDefaults> resolutionDefaults;

    public IntentDictionary(IntentDefinitionDataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource");
        Map<ActionIntent, IntentDefinition> defs = new HashMap<>();
        for (IntentDefinitionDataSource.IntentDefinitionEntry e : dataSource.collectDefinitionsEntries()) {
            defs.put(e.intent(), e.definition());
        }

        Map<ActionIntent, IntentResolutionDefaults> res = new HashMap<>();
        for (IntentDefinitionDataSource.IntentResolutionDefaultEntry e : dataSource.collectResolutionDefaultEntries()) {
            res.put(e.intent(), e.defaults());
        }

        validateIntentCoverageCompleteness(defs, res);

        this.definitions = Collections.unmodifiableMap(new HashMap<>(defs));
        this.resolutionDefaults = Collections.unmodifiableMap(new HashMap<>(res));
    }

    /**
     * fail-fast: “등록 결과” 기준으로 모든 {@link ActionIntent}에 정의·기본값이 있는지 검증한다.
     * (패키지 동일 테스트에서 축소 {@link IntentDefinitionDataSource} 조합 검증에 사용.)
     */
    static void validateIntentCoverageCompleteness(
            Map<ActionIntent, IntentDefinition> defs,
            Map<ActionIntent, IntentResolutionDefaults> res) {
        EnumSet<ActionIntent> allIntents = EnumSet.allOf(ActionIntent.class);
        if (!defs.keySet().containsAll(allIntents) || !res.keySet().containsAll(allIntents)) {
            EnumSet<ActionIntent> missingDefinitions = EnumSet.copyOf(allIntents);
            missingDefinitions.removeAll(defs.keySet());
            EnumSet<ActionIntent> missingDefaults = EnumSet.copyOf(allIntents);
            missingDefaults.removeAll(res.keySet());
            throw new IllegalStateException(
                    "IntentDictionary is incomplete. missingDefinitions=" + missingDefinitions
                            + ", missingDefaults=" + missingDefaults);
        }
    }

    /**
     * Resolution defaults for the given intent.
     * semantic resolution flow에서 사용한다.
     */
    public IntentResolutionDefaults getResolutionDefaults(ActionIntent intent) {
        IntentResolutionDefaults d = resolutionDefaults.get(intent);
        if (d == null) {
            throw new IllegalArgumentException("No resolution defaults for: " + intent);
        }
        return d;
    }

    public Optional<IntentDefinition> get(ActionIntent intent) {
        return Optional.ofNullable(definitions.get(intent));
    }

    public IntentDefinition getOrThrow(ActionIntent intent) {
        IntentDefinition d = definitions.get(intent);
        if (d == null) {
            throw new IllegalArgumentException("No IntentDefinition for: " + intent);
        }
        return d;
    }
}
