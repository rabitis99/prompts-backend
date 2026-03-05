package org.example.sharedprompts.domain.prompt.application.port.in.command;

import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.DomainRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;
import java.util.Objects;

/**
 * 통합 프롬프트 생성 커맨드.
 *
 * <p>외부 DTO 와 1:1 매핑되며, 기본값/폴백은 이 레벨에서 처리한다.</p>
 */
public record UnifiedGeneratePromptCommand(
        Long userId,
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
        CoreRoleType coreRole,
        DomainRoleType domainRole,
        List<String> tags
) {

    public static UnifiedGeneratePromptCommand of(
            Long userId,
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
            CoreRoleType coreRole,
            DomainRoleType domainRole,
            List<String> tags
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        }
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("input은 비어있을 수 없습니다.");
        }

        PromptCategory safeCategory = category != null ? category : PromptCategory.ETC;
        ActionIntent safeIntent = intent != null ? intent : ActionIntent.GENERATE;
        EngineMode safeEngineMode = engineMode != null ? engineMode : EngineMode.AUTO;

        ToneType safeTone = tone != null ? tone : ToneType.NEUTRAL;
        StyleType safeStyle = style != null ? style : StyleType.NARRATIVE;
        LanguageType safeLanguage = language != null ? language : LanguageType.KOREAN;
        ExperienceLevel safeExperience = experience != null ? experience : ExperienceLevel.INTERMEDIATE;

        if (disableQualityPipeline != null && disableQualityPipeline) {
            throw new IllegalArgumentException(
                    "disable_quality_pipeline 옵션은 현재 준비 중입니다. Verify/Repair 파이프라인 비활성화는 추후 지원 예정입니다.");
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

        return new UnifiedGeneratePromptCommand(
                userId,
                safeCategory,
                safeIntent,
                variant,
                input,
                jsonSchema,
                safeEngineMode,
                safeTone,
                safeStyle,
                safeLanguage,
                safeExperience,
                false,
                actionType,
                roleType,
                coreRole,
                domainRole,
                safeTags
        );
    }
}

