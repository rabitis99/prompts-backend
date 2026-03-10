package org.example.sharedprompts.domain.prompt.application.port.in.command.normalization;

import org.example.sharedprompts.domain.prompt.common.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;

/** 표현 옵션 (tone, style) */
public record ExpressionOptions(
        ToneType tone,
        StyleType style,
        LanguageType language,
        ExperienceLevel experienceLevel
) {
    public ExpressionOptions {
        tone = tone != null ? tone : ToneType.NEUTRAL;
        style = style != null ? style : StyleType.NARRATIVE;
        language = language != null ? language : LanguageType.KOREAN;
        experienceLevel = experienceLevel != null ? experienceLevel : ExperienceLevel.INTERMEDIATE;
    }
}
