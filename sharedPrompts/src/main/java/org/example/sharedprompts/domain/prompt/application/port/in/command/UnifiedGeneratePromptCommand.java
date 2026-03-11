package org.example.sharedprompts.domain.prompt.application.port.in.command;

import org.example.sharedprompts.domain.prompt.application.exception.UnsupportedQualityPipelineOptionException;
import org.example.sharedprompts.domain.prompt.application.port.in.command.normalization.ExpressionOptions;
import org.example.sharedprompts.domain.prompt.application.port.in.command.normalization.OutputOptions;
import org.example.sharedprompts.domain.prompt.application.port.in.command.normalization.SemanticSelection;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.experience.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;

import java.util.List;
import java.util.Objects;

/**
 * 통합 프롬프트 생성 커맨드.
 *
 * <p>{@code disableQualityPipeline}: API에서 받을 수 있으나 현재는 미구현이다.
 * true이면 {@link UnsupportedQualityPipelineOptionException}을 던져 명시적으로 거부한다.
 * 파라미터를 유지하는 이유는 향후 품질 파이프라인 비활성화를 지원할 때 API 계약을 바꾸지 않기 위함이다.
 */
public record UnifiedGeneratePromptCommand(
        Long userId,
        RequestMode requestMode,
        PromptCategory category,
        ActionIntent intent,
        String variant,
        String input,
        String jsonSchema,
        EngineMode engineMode,
        ToneType tone,
        StyleType style,
        LanguageType language,
        ExperienceLevel experience,
        boolean disableQualityPipeline,
        ActionTypeInterface actionType,
        RoleTypeInterface roleType,
        List<String> tags,
        String title,
        String description
) {

    public UnifiedGeneratePromptCommand {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        }
        if (requestMode == null) {
            throw new IllegalArgumentException("requestMode는 null일 수 없습니다.");
        }
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("input은 비어있을 수 없습니다.");
        }
        if (requestMode == RequestMode.EXTRACTION && (jsonSchema == null || jsonSchema.isBlank())) {
            throw new IllegalArgumentException("EXTRACTION 모드에서는 jsonSchema가 필수입니다.");
        }

        EngineMode safeEngineMode = engineMode != null ? engineMode : EngineMode.AUTO;

        ToneType safeTone = tone != null ? tone : ToneType.NEUTRAL;
        StyleType safeStyle = style != null ? style : StyleType.NARRATIVE;
        LanguageType safeLanguage = language != null ? language : LanguageType.KOREAN;
        ExperienceLevel safeExperience = experience != null ? experience : ExperienceLevel.INTERMEDIATE;

        // disableQualityPipeline=true는 현재 미지원; 향후 구현 시 계약 유지를 위해 파라미터는 수용 후 거부
        if (disableQualityPipeline) {
            throw new UnsupportedQualityPipelineOptionException();
        }

        List<String> safeTags;
        if (tags == null) {
            safeTags = List.of();
        } else {
            if (tags.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("tags에는 null 값을 포함할 수 없습니다.");
            }
            safeTags = List.copyOf(tags);
        }

        engineMode = safeEngineMode;
        tone = safeTone;
        style = safeStyle;
        language = safeLanguage;
        experience = safeExperience;
        tags = safeTags;
    }

    public boolean isExtractionRequest() {
        return requestMode == RequestMode.EXTRACTION;
    }

    public static UnifiedGeneratePromptCommand fromNormalized(
            Long userId,
            RequestMode requestMode,
            SemanticSelection semantic,
            ExpressionOptions expression,
            OutputOptions output,
            String variant,
            String input,
            boolean disableQualityPipeline,
            List<String> tags,
            String title,
            String description
    ) {
        return new UnifiedGeneratePromptCommand(
                userId,
                requestMode,
                semantic != null ? semantic.category() : null,
                semantic != null ? semantic.intent() : null,
                variant,
                input,
                output != null ? output.jsonSchema() : null,
                output != null ? output.engineMode() : null,
                expression != null ? expression.tone() : null,
                expression != null ? expression.style() : null,
                expression != null ? expression.language() : null,
                expression != null ? expression.experienceLevel() : null,
                disableQualityPipeline,
                semantic != null ? semantic.actionType() : null,
                semantic != null ? semantic.roleType() : null,
                tags != null ? tags : List.of(),
                title,
                description
        );
    }

    public static UnifiedGeneratePromptCommand of(
            Long userId,
            RequestMode requestMode,
            PromptCategory category,
            ActionIntent intent,
            String variant,
            String input,
            String jsonSchema,
            EngineMode engineMode,
            ToneType tone,
            StyleType style,
            LanguageType language,
            ExperienceLevel experience,
            Boolean disableQualityPipeline,
            ActionTypeInterface actionType,
            RoleTypeInterface roleType,
            List<String> tags,
            String title,
            String description
) {
        return new UnifiedGeneratePromptCommand(
                userId,
                requestMode,
                category,
                intent,
                variant,
                input,
                jsonSchema,
                engineMode,
                tone,
                style,
                language,
                experience,
                disableQualityPipeline != null && disableQualityPipeline,
                actionType,
                roleType,
                tags,
                title,
                description
        );
    }

    public static UnifiedGeneratePromptCommand forSimple(
            Long userId,
            PromptCategory category,
            ActionIntent intent,
            String variant,
            String input,
            ToneType tone,
            StyleType style,
            LanguageType language,
            ExperienceLevel experience,
            List<String> tags,
            String title,
            String description
    ) {
        return new UnifiedGeneratePromptCommand(
                userId,
                RequestMode.SIMPLE,
                category,
                intent,
                variant,
                input,
                null,
                null,
                tone,
                style,
                language,
                experience,
                false,
                null,
                null,
                tags,
                title,
                description
        );
    }

    public static UnifiedGeneratePromptCommand forExtraction(
            Long userId,
            String input,
            String jsonSchema,
            ToneType tone,
            StyleType style,
            LanguageType language,
            ExperienceLevel experience,
            List<String> tags,
            String title,
            String description
    ) {
        return new UnifiedGeneratePromptCommand(
                userId,
                RequestMode.EXTRACTION,
                null,
                null,
                null,
                input,
                jsonSchema,
                null,
                tone,
                style,
                language,
                experience,
                false,
                null,
                null,
                tags,
                title,
                description
        );
    }
}

