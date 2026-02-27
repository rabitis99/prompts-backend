package org.example.sharedprompts.domain.prompt.adapter.in.web;

import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.QualityBadge;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BadgeResponseAssembler — 수치 미노출 + 배지 변환 검증")
class BadgeResponseAssemblerTest {

    private BadgeResponseAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new BadgeResponseAssembler();
    }

    @Test
    @DisplayName("GeneratePromptResult → GeneratePromptResponse: 배지만 포함, 수치 없음")
    void assembleConvertsResultToBadgeResponseOnly() {
        GeneratePromptResult result = new GeneratePromptResult(
                10L,
                "테스트 프롬프트",
                "생성된 프롬프트 내용",
                List.of(QualityBadge.CONDITIONS_MET, QualityBadge.FAST_GENERATION),
                PromptObjective.FACTUAL,
                true,   // firstPassSuccess — 내부 지표
                0,      // repairCount — 내부 지표
                true    // finallyPassed — 내부 지표
        );

        GeneratePromptResponse response = assembler.assemble(result);

        // 응답에는 배지만 있어야 함
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.content()).isEqualTo("생성된 프롬프트 내용");
        assertThat(response.qualityBadges()).hasSize(2);

        // 배지 코드와 표시명 확인
        assertThat(response.qualityBadges())
                .extracting(GeneratePromptResponse.BadgeDto::code)
                .containsExactlyInAnyOrder("CONDITIONS_MET", "FAST_GENERATION");

        // 수치 지표 필드 없음 (GeneratePromptResponse는 id, title, content, qualityBadges만 포함)
        // firstPassSuccess, repairCount, finallyPassed 같은 수치 필드가 없는지 리플렉션으로 검증
        java.lang.reflect.RecordComponent[] components = GeneratePromptResponse.class.getRecordComponents();
        java.util.Set<String> fieldNames = new java.util.HashSet<>();
        for (java.lang.reflect.RecordComponent c : components) fieldNames.add(c.getName());

        assertThat(fieldNames).doesNotContainAnyElementsOf(
                List.of("repairCount", "firstPassSuccess", "finallyPassed", "passRate", "repairRate")
        );
    }

    @Test
    @DisplayName("Repair 후 REVERIFIED 배지가 응답에 포함된다")
    void reverifiedBadgeIsIncludedAfterRepair() {
        GeneratePromptResult result = new GeneratePromptResult(
                20L, "타이틀", "내용",
                List.of(QualityBadge.CONDITIONS_MET, QualityBadge.REVERIFIED),
                PromptObjective.REASONING,
                false, 1, true
        );

        GeneratePromptResponse response = assembler.assemble(result);

        assertThat(response.qualityBadges())
                .extracting(GeneratePromptResponse.BadgeDto::code)
                .contains("REVERIFIED");
        assertThat(response.qualityBadges())
                .extracting(GeneratePromptResponse.BadgeDto::code)
                .doesNotContain("FAST_GENERATION");
    }

    @Test
    @DisplayName("배지 없는 결과도 빈 배지 리스트로 정상 변환")
    void emptyBadgesIsHandledGracefully() {
        GeneratePromptResult result = new GeneratePromptResult(
                30L, "타이틀", "내용",
                List.of(),  // 배지 없음
                PromptObjective.PLANNING,
                false, 2, false
        );

        GeneratePromptResponse response = assembler.assemble(result);

        assertThat(response.qualityBadges()).isEmpty();
    }

    @Test
    @DisplayName("배지 displayName이 한국어로 반환된다")
    void badgeDisplayNameIsInKorean() {
        GeneratePromptResult result = new GeneratePromptResult(
                1L, "타이틀", "내용",
                List.of(QualityBadge.NO_PROHIBITED_CONTENT),
                PromptObjective.FACTUAL,
                true, 0, true
        );

        GeneratePromptResponse response = assembler.assemble(result);

        assertThat(response.qualityBadges().get(0).displayName())
                .isEqualTo(QualityBadge.NO_PROHIBITED_CONTENT.getDisplayName());
    }
}
