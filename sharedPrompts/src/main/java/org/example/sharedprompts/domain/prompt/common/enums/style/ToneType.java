package org.example.sharedprompts.domain.prompt.common.enums.style;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nKey;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nRegistry;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nText;
import org.example.sharedprompts.domain.prompt.common.style.ToneStyleNormalizer;

@Getter
public enum ToneType implements StableKeyedEnum {

    FRIENDLY(
            "TONE.FRIENDLY",
            I18nKey.of("tone.friendly.display_name"),
            I18nKey.of("tone.friendly.guideline"),
            2,
            5,
            false
    ),

    FORMAL(
            "TONE.FORMAL",
            I18nKey.of("tone.formal.display_name"),
            I18nKey.of("tone.formal.guideline"),
            5,
            2,
            false
    ),

    HUMOROUS(
            "TONE.HUMOROUS",
            I18nKey.of("tone.humorous.display_name"),
            I18nKey.of("tone.humorous.guideline"),
            2,
            4,
            true
    ),

    MOTIVATIONAL(
            "TONE.MOTIVATIONAL",
            I18nKey.of("tone.motivational.display_name"),
            I18nKey.of("tone.motivational.guideline"),
            3,
            5,
            false
    ),

    CASUAL(
            "TONE.CASUAL",
            I18nKey.of("tone.casual.display_name"),
            I18nKey.of("tone.casual.guideline"),
            1,
            4,
            false
    ),

    PROFESSIONAL(
            "TONE.PROFESSIONAL",
            I18nKey.of("tone.professional.display_name"),
            I18nKey.of("tone.professional.guideline"),
            4,
            2,
            false
    ),

    EMPATHETIC(
            "TONE.EMPATHETIC",
            I18nKey.of("tone.empathic.display_name"),
            I18nKey.of("tone.empathic.guideline"),
            3,
            5,
            false
    ),

    POSITIVE(
            "TONE.POSITIVE",
            I18nKey.of("tone.positive.display_name"),
            I18nKey.of("tone.positive.guideline"),
            3,
            4,
            false
    ),

    INSPIRATIONAL(
            "TONE.INSPIRATIONAL",
            I18nKey.of("tone.inspirational.display_name"),
            I18nKey.of("tone.inspirational.guideline"),
            3,
            5,
            false
    ),

    NEUTRAL(
            "TONE.NEUTRAL",
            I18nKey.of("tone.neutral.display_name"),
            I18nKey.of("tone.neutral.guideline"),
            3,
            2,
            false
    ),

    ENTHUSIASTIC(
            "TONE.ENTHUSIASTIC",
            I18nKey.of("tone.enthusiastic.display_name"),
            I18nKey.of("tone.enthusiastic.guideline"),
            3,
            5,
            false
    ),

    SARCASTIC(
            "TONE.SARCASTIC",
            I18nKey.of("tone.sarcastic.display_name"),
            I18nKey.of("tone.sarcastic.guideline"),
            3,
            1,
            true
    ),

    NEGATIVE(
            "TONE.NEGATIVE",
            I18nKey.of("tone.negative.display_name"),
            I18nKey.of("tone.negative.guideline"),
            3,
            1,
            true
    );

    static {
        I18nRegistry registry = I18nRegistry.global();

        // Display names: 현재는 한국어만 정의되어 있으므로, en/ja는 일단 동일한 값을 사용한다.
        registry.register(I18nKey.of("tone.friendly.display_name"),
                I18nText.of("친근한", "친근한", "친근한"));
        registry.register(I18nKey.of("tone.formal.display_name"),
                I18nText.of("공식적인", "공식적인", "公式的な"));
        registry.register(I18nKey.of("tone.humorous.display_name"),
                I18nText.of("유머러스한", "유머러스한", "ユーモラス"));
        registry.register(I18nKey.of("tone.motivational.display_name"),
                I18nText.of("동기부여형", "동기부여형", "モチベーション"));
        registry.register(I18nKey.of("tone.casual.display_name"),
                I18nText.of("일상적인", "일상적인", "カジュアル"));
        registry.register(I18nKey.of("tone.professional.display_name"),
                I18nText.of("전문적인", "전문적인", "プロフェッショナル"));
        registry.register(I18nKey.of("tone.empathic.display_name"),
                I18nText.of("공감하는", "공감하는", "共感的"));
        registry.register(I18nKey.of("tone.positive.display_name"),
                I18nText.of("긍정적인", "긍정적인", "ポジティブ"));
        registry.register(I18nKey.of("tone.inspirational.display_name"),
                I18nText.of("영감을 주는", "영감을 주는", "インスピレーショナル"));
        registry.register(I18nKey.of("tone.neutral.display_name"),
                I18nText.of("중립적인", "중립적인", "ニュートラル"));
        registry.register(I18nKey.of("tone.enthusiastic.display_name"),
                I18nText.of("열정적인", "열정적인", "エネルギッシュ"));
        registry.register(I18nKey.of("tone.sarcastic.display_name"),
                I18nText.of("비꼬는", "비꼬는", "皮肉的"));
        registry.register(I18nKey.of("tone.negative.display_name"),
                I18nText.of("부정적인", "부정적인", "ネガティブ"));

        // Guidelines: 기존 ko/en/ja 문자열을 그대로 중앙 레지스트리에 등록한다.
        registry.register(I18nKey.of("tone.friendly.guideline"),
                I18nText.of("친근하고 따뜻한 어조", "Friendly, warm tone", "親しみやすく温かい口調"));
        registry.register(I18nKey.of("tone.formal.guideline"),
                I18nText.of("격식 있고 정중한 어조", "Formal, polite tone", "正式で丁寧な口調"));
        registry.register(I18nKey.of("tone.humorous.guideline"),
                I18nText.of("적절한 가벼운 유머", "Light, appropriate humor", "適切な軽いユーモア"));
        registry.register(I18nKey.of("tone.motivational.guideline"),
                I18nText.of("행동 유도, 긍정적 결과 강조", "Encourage action, highlight positive outcomes",
                        "行動を促し、前向きな結果を強調"));
        registry.register(I18nKey.of("tone.casual.guideline"),
                I18nText.of("자연스러운 일상 대화체", "Natural, conversational tone", "自然な会話調"));
        registry.register(I18nKey.of("tone.professional.guideline"),
                I18nText.of("전문적이고 정확한 어조", "Professional, precise tone", "専門的で正確な口調"));
        registry.register(I18nKey.of("tone.empathic.guideline"),
                I18nText.of("사용자 감정/상황 이해", "Acknowledge user's feelings and situation",
                        "利用者の感情・状況を理解"));
        registry.register(I18nKey.of("tone.positive.guideline"),
                I18nText.of("낙관적이고 긍정적인 어조", "Optimistic, positive tone", "前向きでポジティブな口調"));
        registry.register(I18nKey.of("tone.inspirational.guideline"),
                I18nText.of("창의성과 새 아이디어 자극", "Stimulate creativity and new ideas",
                        "創造性と新しい発想を刺激"));
        registry.register(I18nKey.of("tone.neutral.guideline"),
                I18nText.of("객관적이고 중립적인 어조", "Objective, neutral tone", "客観的で中立な口調"));
        registry.register(I18nKey.of("tone.enthusiastic.guideline"),
                I18nText.of("에너지 넘치고 열정적인 어조", "Energetic, enthusiastic tone",
                        "エネルギッシュで熱意のある口調"));
        registry.register(I18nKey.of("tone.sarcastic.guideline"),
                I18nText.of("빈정대고 풍자적인 어조", "Sarcastic, satirical tone", "皮肉で風刺的な口調"));
        registry.register(I18nKey.of("tone.negative.guideline"),
                I18nText.of("비판적이고 회의적인 어조", "Critical, skeptical tone", "批判的で懐疑的な口調"));
    }

    /** Stable serialization-safe identifier */
    private final String key;

    /** UI 표시용 (다국어 키) */
    private final I18nKey displayNameKey;

    /** Tone guideline per language (다국어 키) */
    private final I18nKey guidelineKey;

    /** 형식성 레벨 (1: 매우 캐주얼 ~ 5: 매우 포멀) */
    private final int formalityLevel;
    /** 따뜻함/정서적 친밀감 레벨 (1: 차가움 ~ 5: 매우 따뜻함) */
    private final int warmthLevel;
    /** 민감한 주제에서 사용 시 리스크가 높은 톤인지 여부 */
    private final boolean riskyForSensitiveTopics;

    ToneType(
            String key,
            I18nKey displayNameKey,
            I18nKey guidelineKey,
            int formalityLevel,
            int warmthLevel,
            boolean riskyForSensitiveTopics
    ) {
        this.key = key;
        this.displayNameKey = displayNameKey;
        this.guidelineKey = guidelineKey;
        this.formalityLevel = formalityLevel;
        this.warmthLevel = warmthLevel;
        this.riskyForSensitiveTopics = riskyForSensitiveTopics;
    }

    @Override
    public String key() {
        return key;
    }

    /**
     * UI 기본 표시는 기존과 동일하게 한국어를 사용한다.
     * (추후 필요 시 LanguageType을 파라미터로 받는 메서드로 확장 가능)
     */
    public String getDisplayName() {
        return I18nRegistry.global().lookup(displayNameKey, LanguageType.KOREAN);
    }

    /**
     * 언어 타입에 따라 적절한 톤 가이드라인을 반환한다.
     */
    public String getGuidelineByLang(LanguageType lang) {
        return I18nRegistry.global().lookup(guidelineKey, lang);
    }

    /**
     * 영어 기준 톤 가이드라인을 반환한다.
     * (기존 호출부의 getGuidelineEn()을 대체하기 위한 헬퍼)
     */
    public String getGuidelineEn() {
        return getGuidelineByLang(LanguageType.ENGLISH);
    }

    /**
     * @deprecated Use {@link ToneStyleNormalizer#toCanonicalTone(ToneType)}. Normalization is policy, not identity.
     */
    @Deprecated(since = "enum-cleanup", forRemoval = true)
    public ToneType getCanonicalTone() {
        return ToneStyleNormalizer.toCanonicalTone(this);
    }
}
