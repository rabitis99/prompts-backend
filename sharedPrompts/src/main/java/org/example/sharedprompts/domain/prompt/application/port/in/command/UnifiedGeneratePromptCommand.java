package org.example.sharedprompts.domain.prompt.application.port.in.command;

import org.example.sharedprompts.domain.prompt.application.exception.UnsupportedQualityPipelineOptionException;
import org.example.sharedprompts.domain.prompt.application.port.in.command.normalization.ExpressionOptions;
import org.example.sharedprompts.domain.prompt.application.port.in.command.normalization.OutputOptions;
import org.example.sharedprompts.domain.prompt.application.port.in.command.normalization.SemanticSelection;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;
import java.util.Objects;

/**
 * 통합 프롬프트 생성 커맨드.
 *
 * <p>Semantic hierarchy (resolution order): category → intent → roleType/actionType → tone/style → output.
 * Category is required for SIMPLE/ADVANCED; intent may be provided explicitly or resolved from a
 * category-profile fallback. Role/action are optional and validated against the resolved
 * category+intent in {@link org.example.sharedprompts.domain.prompt.application.service.semantic.SemanticValidationService}.
 * Tone and style are expression modifiers only and do not drive semantic resolution.</p>
 *
 * <p>{@link #requestMode()} is set by the controller from request_type so that
 * semantic resolution does not rely on category=null heuristics.</p>
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

        // Do NOT default category or intent here; semantic resolution requires explicit or profile fallback.
        EngineMode safeEngineMode = engineMode != null ? engineMode : EngineMode.AUTO;

        ToneType safeTone = tone != null ? tone : ToneType.NEUTRAL;
        StyleType safeStyle = style != null ? style : StyleType.NARRATIVE;
        LanguageType safeLanguage = language != null ? language : LanguageType.KOREAN;
        ExperienceLevel safeExperience = experience != null ? experience : ExperienceLevel.INTERMEDIATE;

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

    /**
     * True when this command represents an EXTRACTION request.
     * Prefer using {@link #requestMode()} instead of this heuristic.
     */
    public boolean isExtractionRequest() {
        return requestMode == RequestMode.EXTRACTION;
    }

    /**
     * Hierarchy-aware factory: builds command from semantic selection, expression options, and output options.
     * Use this when normalizing requests so the internal model does not behave as a flat enum bag.
     */
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

    /**
     * 단순 프롬프트 생성 요청을 위한 전용 팩토리.
     */
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

    /**
     * 추출(EXTRACTION) 모드 요청을 위한 전용 팩토리.
     * category와 intent는 null; semantic resolution이 EXTRACT로 고정한다.
     */
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
                tone != null ? tone : ToneType.NEUTRAL,
                style != null ? style : StyleType.NARRATIVE,
                language,
                experience,
                false,
                null,
                null,
                tags != null ? tags : List.of(),
                title,
                description
        );
    }
}

