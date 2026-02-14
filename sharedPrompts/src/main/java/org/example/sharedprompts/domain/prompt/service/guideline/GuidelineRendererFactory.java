package org.example.sharedprompts.domain.prompt.service.guideline;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.springframework.stereotype.Component;

/**
 * LanguageType에 따라 적절한 GuidelineRenderer를 반환하는 팩토리
 */
@Component
@RequiredArgsConstructor
public class GuidelineRendererFactory {

    private final KoreanGuidelineRenderer koreanRenderer;
    private final EnglishGuidelineRenderer englishRenderer;
    private final JapaneseGuidelineRenderer japaneseRenderer;

    public GuidelineRenderer getRenderer(LanguageType languageType) {
        return switch (languageType) {
            case KOREAN -> koreanRenderer;
            case ENGLISH -> englishRenderer;
            case JAPANESE -> japaneseRenderer;
        };
    }
}

