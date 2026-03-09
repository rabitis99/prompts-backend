package org.example.sharedprompts.domain.prompt.application.service.generate;

import org.example.sharedprompts.domain.prompt.adapter.in.web.mapper.ConfirmedAxesMapper;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.ConfirmedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;
import org.example.sharedprompts.domain.prompt.common.AxisSourceConstants;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeneratePromptFromConfirmedAxesServiceTest {

    private ConfirmedAxesMapper confirmedAxesMapper;
    private CategorySemanticProfileRegistry profileRegistry;
    private GeneratePromptUseCase generatePromptUseCase;
    private GeneratePromptFromConfirmedAxesService service;

    @BeforeEach
    void setUp() {
        confirmedAxesMapper = new ConfirmedAxesMapper();
        profileRegistry = mock(CategorySemanticProfileRegistry.class);
        generatePromptUseCase = mock(GeneratePromptUseCase.class);

        service = new GeneratePromptFromConfirmedAxesService(
                confirmedAxesMapper, profileRegistry, generatePromptUseCase
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
}
