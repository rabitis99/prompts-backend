package org.example.sharedprompts.domain.prompt.application.service;

import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.out.ConstrainedDecodingPort;
import org.example.sharedprompts.domain.prompt.application.port.out.LLMClientPort;
import org.example.sharedprompts.domain.prompt.application.port.out.PromptSpecRendererPort;
import org.example.sharedprompts.domain.prompt.application.port.out.SavePromptVersionPort;
import org.example.sharedprompts.domain.prompt.application.port.out.ValidateUserPort;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.model.VerifyResult;
import org.example.sharedprompts.domain.prompt.domain.objective.DefaultObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.AnalyticalObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.CreativeObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.ExtractionObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.FactualObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.PlanningObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.ReasoningObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.policy.StrategyBundlePolicy;
import org.example.sharedprompts.domain.prompt.domain.resolution.DomainResolver;
import org.example.sharedprompts.domain.prompt.domain.resolution.DomainResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolution.ExplicitObjectiveMapping;
import org.example.sharedprompts.domain.prompt.domain.resolution.ObjectiveMappingRegistry;
import org.example.sharedprompts.domain.prompt.domain.resolution.ObjectiveResolver;
import org.example.sharedprompts.domain.prompt.domain.service.BadgeResolver;
import org.example.sharedprompts.domain.prompt.domain.service.PromptSpecFactory;
import org.example.sharedprompts.domain.prompt.domain.service.PromptSpecValidator;
import org.example.sharedprompts.domain.prompt.domain.value.QualityBadge;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.example.sharedprompts.domain.prompt.enums.action.EtcActionType;
import org.example.sharedprompts.domain.prompt.enums.role.EtcRoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeneratePromptService 단위 테스트")
class GeneratePromptServiceTest {

    @Mock private LLMClientPort llmClientPort;
    @Mock private ConstrainedDecodingPort constrainedDecodingPort;
    @Mock private SavePromptVersionPort savePromptVersionPort;
    @Mock private ValidateUserPort validateUserPort;
    @Mock private PromptSpecValidator mockValidator;
    @Mock private PromptSpecRendererPort promptSpecRenderer;

    private GeneratePromptService service;
    private GeneratePromptCommand command;

    @BeforeEach
    void setUp() {
        ObjectiveRegistry registry = new DefaultObjectiveRegistry(java.util.List.of(
                new FactualObjectiveProfile(),
                new ReasoningObjectiveProfile(),
                new ExtractionObjectiveProfile(),
                new PlanningObjectiveProfile(),
                new CreativeObjectiveProfile(),
                new AnalyticalObjectiveProfile()
        ));
        StrategyBundlePolicy bundlePolicy = new StrategyBundlePolicy(registry);
        PromptSpecFactory factory = new PromptSpecFactory(registry, bundlePolicy,
                new ObjectiveResolver(new ExplicitObjectiveMapping(), new ObjectiveMappingRegistry()));
        DomainResolverPort domainResolver = new DomainResolver();
        BadgeResolver badgeResolver = new BadgeResolver();

        service = new GeneratePromptService(
                factory, mockValidator, domainResolver,
                llmClientPort, constrainedDecodingPort, validateUserPort, savePromptVersionPort, promptSpecRenderer,
                registry, badgeResolver
        );

        command = new GeneratePromptCommand(
                1L, "테스트 프롬프트", "설명", true,
                PromptCategory.DEVELOPMENT,
                List.of("태그1"),
                "AI 관련 내용을 분석해 주세요.",
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL, StyleType.NARRATIVE, LanguageType.KOREAN,
                false, null
        );
    }

    @Test
    @DisplayName("First-pass 성공: Repair 없이 FAST_GENERATION 배지 반환")
    void firstPassSuccessReturnsFastGenerationBadge() {
        String draft = "AI 분야의 최신 트렌드를 분석합니다. 각 기술 영역의 발전 방향을 살펴보겠습니다.";
        given(llmClientPort.solve(any(PromptSpec.class))).willReturn(draft);
        given(mockValidator.verify(eq(draft), any(PromptSpec.class)))
                .willReturn(VerifyResult.pass(Map.of(
                        QualityRubric.RubricItem.COVERAGE, true,
                        QualityRubric.RubricItem.NO_PROHIBITED_CONTENT, true,
                        QualityRubric.RubricItem.FORMAT_COMPLIANCE, true
                )));
        given(savePromptVersionPort.save(any(), any(), anyString(), anyInt(), eq(true)))
                .willReturn(42L);

        GeneratePromptResult result = service.generate(command);

        assertThat(result.firstPassSuccess()).isTrue();
        assertThat(result.repairCount()).isZero();
        assertThat(result.badges()).contains(QualityBadge.FAST_GENERATION);
        assertThat(result.badges()).contains(QualityBadge.CONDITIONS_MET);

        // Repair 호출 없음
        verify(llmClientPort, never()).repair(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Repair 2회 제한 — 무한루프 방지")
    void repairMaxTwoTimesAndStop() {
        String draft = "초안 내용입니다. 검증 실패 예정.";
        String repairedDraft = "수정된 초안입니다. 계속 실패.";

        given(llmClientPort.solve(any(PromptSpec.class))).willReturn(draft);
        given(llmClientPort.repair(any(), any(), any(), any())).willReturn(repairedDraft);

        VerifyResult failResult = VerifyResult.fail(
                Map.of(
                        QualityRubric.RubricItem.COVERAGE, false,
                        QualityRubric.RubricItem.NO_PROHIBITED_CONTENT, true
                ),
                List.of("커버리지 부족")
        );
        given(mockValidator.verify(any(), any(PromptSpec.class))).willReturn(failResult);
        given(savePromptVersionPort.save(any(), any(), anyString(), anyInt(), eq(false)))
                .willReturn(99L);

        GeneratePromptResult result = service.generate(command);

        // Repair는 정확히 2회만 호출
        verify(llmClientPort, times(2)).repair(any(), any(), any(), any());
        assertThat(result.repairCount()).isEqualTo(2);
        assertThat(result.finallyPassed()).isFalse();
        // FAST_GENERATION 배지 없음
        assertThat(result.badges()).doesNotContain(QualityBadge.FAST_GENERATION);
    }

    @Test
    @DisplayName("Repair 1회 후 성공: REVERIFIED 배지 반환")
    void repairOnceSuccessReturnsReverifiedBadge() {
        String draft = "초안 내용 검증 실패";
        String repairedDraft = "수정된 초안 충분히 길고 올바른 내용으로 수정되었습니다. 분석 결과를 포함합니다.";

        given(llmClientPort.solve(any(PromptSpec.class))).willReturn(draft);
        given(llmClientPort.repair(any(), any(), any(), any())).willReturn(repairedDraft);

        VerifyResult failResult = VerifyResult.fail(
                Map.of(QualityRubric.RubricItem.COVERAGE, false,
                       QualityRubric.RubricItem.NO_PROHIBITED_CONTENT, true),
                List.of("커버리지 부족")
        );
        VerifyResult passResult = VerifyResult.pass(Map.of(
                QualityRubric.RubricItem.COVERAGE, true,
                QualityRubric.RubricItem.NO_PROHIBITED_CONTENT, true
        ));

        given(mockValidator.verify(eq(draft), any())).willReturn(failResult);
        given(mockValidator.verify(eq(repairedDraft), any())).willReturn(passResult);
        given(savePromptVersionPort.save(any(), any(), anyString(), anyInt(), eq(true)))
                .willReturn(55L);

        GeneratePromptResult result = service.generate(command);

        assertThat(result.repairCount()).isEqualTo(1);
        assertThat(result.finallyPassed()).isTrue();
        assertThat(result.badges()).contains(QualityBadge.REVERIFIED);
        assertThat(result.badges()).doesNotContain(QualityBadge.FAST_GENERATION);
    }

    @Test
    @DisplayName("응답에 pass rate/repair count 등 수치는 Result 필드에 있으나 배지만 노출 대상")
    void resultContainsInternalMetricsButBadgesAreUxFacing() {
        String draft = "충분한 내용의 초안입니다. AI 기술 분석을 수행하고 결과를 제공합니다.";
        given(llmClientPort.solve(any())).willReturn(draft);
        given(mockValidator.verify(any(), any())).willReturn(VerifyResult.pass(Map.of(
                QualityRubric.RubricItem.COVERAGE, true,
                QualityRubric.RubricItem.NO_PROHIBITED_CONTENT, true
        )));
        given(savePromptVersionPort.save(any(), any(), anyString(), anyInt(), eq(true))).willReturn(1L);

        GeneratePromptResult result = service.generate(command);

        // 내부 지표는 Result에 존재
        assertThat(result.firstPassSuccess()).isTrue();
        assertThat(result.repairCount()).isZero();
        // 배지만 UX 응답 변환 대상
        assertThat(result.badges()).isNotEmpty();
    }
}
