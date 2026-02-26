package org.example.sharedprompts.domain.prompt.domain.service;

import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecommendationRegistry 단위 테스트")
class RecommendationRegistryTest {

    private final RecommendationRegistry registry = new RecommendationRegistry();

    @Test
    @DisplayName("GENERAL 도메인은 모든 ToneType과 StyleType을 추천한다")
    void generalDomainRecommendsAllTonesAndStyles() {
        Set<ToneType> tones = registry.getRecommendedTones(TaskDomain.GENERAL);
        Set<StyleType> styles = registry.getRecommendedStyles(TaskDomain.GENERAL);

        assertThat(tones).containsExactlyInAnyOrder(ToneType.values());
        assertThat(styles).containsExactlyInAnyOrder(StyleType.values());
    }

    @Test
    @DisplayName("TECHNICAL 도메인 추천에는 PROFESSIONAL/FORMAL/NEUTRAL 및 TECHNICAL 스타일이 포함된다")
    void technicalDomainProfileContainsExpectedTonesAndStyles() {
        Set<ToneType> tones = registry.getRecommendedTones(TaskDomain.TECHNICAL);
        Set<StyleType> styles = registry.getRecommendedStyles(TaskDomain.TECHNICAL);

        assertThat(tones).contains(ToneType.PROFESSIONAL, ToneType.FORMAL, ToneType.NEUTRAL);
        assertThat(styles).contains(StyleType.TECHNICAL);
    }

    @Test
    @DisplayName("도메인 프로파일에는 Objective bias가 세팅되어 있다")
    void domainProfileHasObjectiveBias() {
        RecommendationRegistry.DomainProfile technical = registry.getProfile(TaskDomain.TECHNICAL);

        assertThat(technical.objectiveBias())
                .containsKeys(PromptObjective.REASONING, PromptObjective.FACTUAL);
    }
}

