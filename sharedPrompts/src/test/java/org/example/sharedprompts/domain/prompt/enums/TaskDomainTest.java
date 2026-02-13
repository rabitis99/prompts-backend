package org.example.sharedprompts.domain.prompt.enums;

import org.example.sharedprompts.domain.prompt.enums.guideline.GuidelineRule;
import org.example.sharedprompts.domain.prompt.enums.guideline.I18nText;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TaskDomain enum 테스트
 * <p>각 도메인 가이드라인이 비어있지 않고, I18nText가 null이 아닌지 검증</p>
 */
@DisplayName("TaskDomain 테스트")
class TaskDomainTest {

    @Test
    @DisplayName("모든 도메인의 핵심 원칙이 비어있지 않아야 함")
    void allDomainsHaveNonEmptyPrinciples() {
        for (TaskDomain domain : TaskDomain.values()) {
            List<GuidelineRule> principles = domain.principles();
            assertThat(principles)
                    .as("%s 도메인의 핵심 원칙", domain)
                    .isNotEmpty();
        }
    }

    @Test
    @DisplayName("모든 도메인의 구조화 규칙이 비어있지 않아야 함")
    void allDomainsHaveNonEmptyStructuringRules() {
        for (TaskDomain domain : TaskDomain.values()) {
            List<GuidelineRule> rules = domain.structuringRules();
            assertThat(rules)
                    .as("%s 도메인의 구조화 규칙", domain)
                    .isNotEmpty();
        }
    }

    @Test
    @DisplayName("모든 도메인의 품질 기준이 비어있지 않아야 함")
    void allDomainsHaveNonEmptyQualityStandards() {
        for (TaskDomain domain : TaskDomain.values()) {
            List<GuidelineRule> standards = domain.qualityStandards();
            assertThat(standards)
                    .as("%s 도메인의 품질 기준", domain)
                    .isNotEmpty();
        }
    }

    @Test
    @DisplayName("모든 도메인의 출력 제약이 비어있지 않아야 함")
    void allDomainsHaveNonEmptyOutputConstraints() {
        for (TaskDomain domain : TaskDomain.values()) {
            List<GuidelineRule> constraints = domain.outputConstraints();
            assertThat(constraints)
                    .as("%s 도메인의 출력 제약", domain)
                    .isNotEmpty();
        }
    }

    @Test
    @DisplayName("모든 규칙의 I18nText가 null이 아니어야 함")
    void allRulesHaveNonNullI18nText() {
        for (TaskDomain domain : TaskDomain.values()) {
            List<GuidelineRule> allRules = List.of(
                    domain.principles(),
                    domain.structuringRules(),
                    domain.qualityStandards(),
                    domain.outputConstraints()
            ).stream()
                    .flatMap(List::stream)
                    .toList();

            for (GuidelineRule rule : allRules) {
                I18nText title = rule.title();
                I18nText description = rule.description();

                assertThat(title)
                        .as("%s 도메인의 규칙 %s의 제목", domain, rule.id())
                        .isNotNull();
                assertThat(title.ko())
                        .as("%s 도메인의 규칙 %s의 한국어 제목", domain, rule.id())
                        .isNotNull()
                        .isNotBlank();
                assertThat(title.en())
                        .as("%s 도메인의 규칙 %s의 영어 제목", domain, rule.id())
                        .isNotNull()
                        .isNotBlank();
                assertThat(title.ja())
                        .as("%s 도메인의 규칙 %s의 일본어 제목", domain, rule.id())
                        .isNotNull()
                        .isNotBlank();

                assertThat(description)
                        .as("%s 도메인의 규칙 %s의 설명", domain, rule.id())
                        .isNotNull();
                assertThat(description.ko())
                        .as("%s 도메인의 규칙 %s의 한국어 설명", domain, rule.id())
                        .isNotNull()
                        .isNotBlank();
                assertThat(description.en())
                        .as("%s 도메인의 규칙 %s의 영어 설명", domain, rule.id())
                        .isNotNull()
                        .isNotBlank();
                assertThat(description.ja())
                        .as("%s 도메인의 규칙 %s의 일본어 설명", domain, rule.id())
                        .isNotNull()
                        .isNotBlank();
            }
        }
    }

    @Test
    @DisplayName("모든 규칙의 id가 고유해야 함")
    void allRuleIdsAreUnique() {
        List<String> allIds = List.of(TaskDomain.values()).stream()
                .flatMap(domain -> List.of(
                        domain.principles(),
                        domain.structuringRules(),
                        domain.qualityStandards(),
                        domain.outputConstraints()
                ).stream())
                .flatMap(List::stream)
                .map(GuidelineRule::id)
                .toList();

        assertThat(allIds)
                .as("모든 규칙의 id는 고유해야 함")
                .doesNotHaveDuplicates();
    }
}

