package org.example.sharedprompts.domain.prompt.application.engine.generation;

import org.example.sharedprompts.domain.prompt.application.engine.contract.SchemaContractEvaluator;
import org.example.sharedprompts.domain.prompt.application.mapping.ConfirmedAxesMapper;
import org.example.sharedprompts.domain.prompt.application.policy.AxisSourcePolicy;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.GeneratePromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.ConfirmedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.semantic.IntentBasedAxisDefaultsResolver;
import org.example.sharedprompts.domain.prompt.application.semantic.validation.SemanticValidationService;
import org.example.sharedprompts.domain.prompt.common.AxisSourceConstants;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.application.exception.SemanticResolutionException;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeneratePromptFromConfirmedAxesServiceTest {

    private ConfirmedAxesMapper confirmedAxesMapper;
    private CategorySemanticProfileRegistry profileRegistry;
    private SemanticValidationService validationService;
    private IntentBasedAxisDefaultsResolver axisDefaultsResolver;
    private GeneratePromptUseCase generatePromptUseCase;
    private AxisSourcePolicy axisSourcePolicy;
    private UnifiedGeneratePromptResultBuilder resultBuilder;
    private SchemaContractEvaluator schemaContractEvaluator;
    private GeneratePromptFromConfirmedAxesService service;

    @BeforeEach
    void setUp() {
        confirmedAxesMapper = new ConfirmedAxesMapper();
        profileRegistry = mock(CategorySemanticProfileRegistry.class);
        validationService = mock(SemanticValidationService.class);
        axisDefaultsResolver = mock(IntentBasedAxisDefaultsResolver.class);
        generatePromptUseCase = mock(GeneratePromptUseCase.class);
        axisSourcePolicy = mock(AxisSourcePolicy.class);
        resultBuilder = mock(UnifiedGeneratePromptResultBuilder.class);
        schemaContractEvaluator = mock(SchemaContractEvaluator.class);

        when(validationService.validate(any(), any())).thenReturn(SemanticValidationResult.success());
        when(schemaContractEvaluator.evaluate(any())).thenReturn(
                new SchemaContractEvaluator.SchemaContractEvaluation(false, List.of()));
        when(axisSourcePolicy.forConfirmedAxes(any())).thenReturn(Map.of(
                "intent", AxisSourceConstants.USER_PROVIDED,
                "role", AxisSourceConstants.USER_PROVIDED,
                "action", AxisSourceConstants.USER_PROVIDED,
                "objective", AxisSourceConstants.RECOMMENDED,
                "output_needs", AxisSourceConstants.RECOMMENDED
        ));
        when(resultBuilder.build(any(), any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Map<String, String> axisSources = inv.getArgument(5);
            return new UnifiedGeneratePromptResult(
                    "output content", EngineMode.AUTO, EngineMode.V2, PromptCategory.ETC, null, null, null,
                    ActionIntent.GENERATE, null, null, null, List.of(), true, 0, true, false, List.of(),
                    EngineProfile.QUALITY_PIPELINE, List.of(), List.of(), List.of(), "confirmed", axisSources);
        });

        service = new GeneratePromptFromConfirmedAxesService(
                confirmedAxesMapper, profileRegistry, validationService, axisDefaultsResolver,
                generatePromptUseCase, axisSourcePolicy, resultBuilder, schemaContractEvaluator
        );
    }

    @Test
    void generate_confirmedAxesProduceValidPrompt_andObjectiveDerived() {
        ConfirmedGeneratePromptCommand command = new ConfirmedGeneratePromptCommand(
                1L, RequestMode.ADVANCED, PromptCategory.ETC, ActionIntent.GENERATE, null, null,
                ToneType.NEUTRAL, StyleType.NARRATIVE, LanguageType.KOREAN, ExperienceLevel.INTERMEDIATE,
                "input text", null, "Title", "Desc", List.of()
        );

        when(profileRegistry.getProfile(PromptCategory.ETC)).thenReturn(Optional.empty());

        when(generatePromptUseCase.generate(any(), any())).thenReturn(
                new GeneratePromptResult(1L, "Title", "output content", List.of(), null, true, true, 0, true)
        );

        UnifiedGeneratePromptResult result = service.generate(command);

        verify(validationService).validate(any(), any());
        ArgumentCaptor<ConfirmedSemanticAxes> axesCaptor = ArgumentCaptor.forClass(ConfirmedSemanticAxes.class);
        verify(generatePromptUseCase).generate(any(GeneratePromptCommand.class), axesCaptor.capture());

        ConfirmedSemanticAxes resolvedAxes = axesCaptor.getValue();
        assertThat(resolvedAxes.category()).isEqualTo(PromptCategory.ETC);
        assertThat(resolvedAxes.intent()).isEqualTo(ActionIntent.GENERATE);

        // Objective and outputNeeds should be derived from IntentDictionary
        assertThat(resolvedAxes.objective()).isNotNull();
        assertThat(resolvedAxes.outputNeeds()).isNotNull();

        assertThat(result.axisSources()).containsEntry("intent", AxisSourceConstants.USER_PROVIDED);
        assertThat(result.axisSources()).containsEntry("role", AxisSourceConstants.USER_PROVIDED);
        assertThat(result.axisSources()).containsEntry("action", AxisSourceConstants.USER_PROVIDED);
        assertThat(result.axisSources()).containsEntry("objective", AxisSourceConstants.RECOMMENDED);
        assertThat(result.axisSources()).containsEntry("output_needs", AxisSourceConstants.RECOMMENDED);
    }

    @Test
    void generate_whenValidationFails_throwsSemanticResolutionException_andDoesNotCallUseCase() {
        ConfirmedGeneratePromptCommand command = new ConfirmedGeneratePromptCommand(
                1L, RequestMode.ADVANCED, PromptCategory.ETC, ActionIntent.GENERATE, null, null,
                ToneType.NEUTRAL, StyleType.NARRATIVE, LanguageType.KOREAN, ExperienceLevel.INTERMEDIATE,
                "input text", null, "Title", "Desc", List.of()
        );

        when(profileRegistry.getProfile(PromptCategory.ETC)).thenReturn(Optional.empty());
        when(validationService.validate(any(), any())).thenReturn(
                SemanticValidationResult.invalid(List.of(
                        new SemanticValidationResult.SemanticValidationItem("INVALID_AXIS", "Invalid combination", "category", null)
                ))
        );

        assertThatThrownBy(() -> service.generate(command))
                .isInstanceOf(SemanticResolutionException.class)
                .hasMessageContaining("INVALID_AXIS")
                .hasMessageContaining("Invalid combination");

        verify(validationService).validate(any(), any());
        verify(generatePromptUseCase, never()).generate(any(), any());
    }
}
