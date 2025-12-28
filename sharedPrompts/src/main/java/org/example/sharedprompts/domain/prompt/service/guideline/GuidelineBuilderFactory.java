package org.example.sharedprompts.domain.prompt.service.guideline;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GuidelineBuilderFactory {

    private final EnglishGuidelineBuilder englishBuilder;
    private final KoreanGuidelineBuilder koreanBuilder;
    private final JapaneseGuidelineBuilder japaneseBuilder;

    public PromptGuidelineBuilder getBuilder(LanguageType languageType) {
        return switch (languageType) {
            case ENGLISH -> englishBuilder;
            case KOREAN -> koreanBuilder;
            case JAPANESE -> japaneseBuilder;
        };
    }
}