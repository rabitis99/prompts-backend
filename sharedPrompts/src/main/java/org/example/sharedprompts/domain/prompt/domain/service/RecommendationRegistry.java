package org.example.sharedprompts.domain.prompt.domain.service;

import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * TaskDomain별 톤·스타일 추천 프로파일 레지스트리.
 *
 * <p>Spring 의존 없음 — {@code PromptDomainConfig}에서 @Bean으로 등록한다.
 */
public class RecommendationRegistry {

    public record DomainProfile(
            Set<ToneType> recommendedTones,
            Set<StyleType> recommendedStyles,
            Map<PromptObjective, Double> objectiveBias
    ) {}

    private final Map<TaskDomain, DomainProfile> profiles = buildProfiles();

    private Map<TaskDomain, DomainProfile> buildProfiles() {
        Map<TaskDomain, DomainProfile> map = new EnumMap<>(TaskDomain.class);

        map.put(TaskDomain.TECHNICAL, new DomainProfile(
                EnumSet.of(ToneType.PROFESSIONAL, ToneType.FORMAL, ToneType.NEUTRAL),
                EnumSet.of(StyleType.TECHNICAL, StyleType.FORMATTED, StyleType.INSTRUCTIVE, StyleType.CONCISE),
                Map.of(PromptObjective.REASONING, 0.6, PromptObjective.FACTUAL, 0.4)
        ));

        map.put(TaskDomain.CREATIVE, new DomainProfile(
                EnumSet.of(ToneType.INSPIRATIONAL, ToneType.ENTHUSIASTIC, ToneType.FRIENDLY, ToneType.CASUAL),
                EnumSet.of(StyleType.NARRATIVE, StyleType.STORYTELLING, StyleType.DESCRIPTIVE, StyleType.CREATIVE, StyleType.DIALOGUE),
                Map.of(PromptObjective.CREATIVE_WITH_CONSTRAINTS, 1.0)
        ));

        map.put(TaskDomain.ANALYTICAL, new DomainProfile(
                EnumSet.of(ToneType.NEUTRAL, ToneType.PROFESSIONAL, ToneType.FORMAL),
                EnumSet.of(StyleType.ANALYTICAL, StyleType.COMPARATIVE, StyleType.QUESTION_ANSWER),
                Map.of(PromptObjective.FACTUAL, 0.7, PromptObjective.REASONING, 0.3)
        ));

        map.put(TaskDomain.PRACTICAL, new DomainProfile(
                EnumSet.of(ToneType.PROFESSIONAL, ToneType.FRIENDLY, ToneType.MOTIVATIONAL, ToneType.POSITIVE),
                EnumSet.of(StyleType.BULLET, StyleType.CONCISE, StyleType.INSTRUCTIVE, StyleType.FORMATTED),
                Map.of(PromptObjective.PLANNING, 1.0)
        ));

        map.put(TaskDomain.EDUCATIONAL, new DomainProfile(
                EnumSet.of(ToneType.FRIENDLY, ToneType.MOTIVATIONAL, ToneType.ENTHUSIASTIC, ToneType.EMPATHETIC),
                EnumSet.of(StyleType.INSTRUCTIVE, StyleType.DETAILED, StyleType.QUESTION_ANSWER),
                Map.of(PromptObjective.REASONING, 1.0)
        ));

        map.put(TaskDomain.GENERAL, new DomainProfile(
                EnumSet.allOf(ToneType.class),
                EnumSet.allOf(StyleType.class),
                Map.of(PromptObjective.REASONING, 1.0)
        ));

        return Map.copyOf(map);
    }

    public DomainProfile getProfile(TaskDomain domain) {
        DomainProfile general = profiles.get(TaskDomain.GENERAL);
        if (domain == null) return general;
        return profiles.getOrDefault(domain, general);
    }

    public Set<ToneType> getRecommendedTones(TaskDomain domain) {
        return Set.copyOf(getProfile(domain).recommendedTones());
    }

    public Set<StyleType> getRecommendedStyles(TaskDomain domain) {
        return Set.copyOf(getProfile(domain).recommendedStyles());
    }

    public Set<TaskDomain> getRecommendedDomainsForTone(ToneType tone) {
        return profiles.entrySet().stream()
                .filter(e -> e.getValue().recommendedTones().contains(tone))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public Set<TaskDomain> getRecommendedDomainsForStyle(StyleType style) {
        return profiles.entrySet().stream()
                .filter(e -> e.getValue().recommendedStyles().contains(style))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}

