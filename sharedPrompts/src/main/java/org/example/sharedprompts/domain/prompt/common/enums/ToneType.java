package org.example.sharedprompts.domain.prompt.common.enums;

import lombok.Getter;

import static org.example.sharedprompts.domain.prompt.common.enums.I18nUtils.getByLang;

@Getter
public enum ToneType {

    FRIENDLY(
            "친근한",
            "친근하고 따뜻한 어조",
            "Friendly, warm tone",
            "親しみやすく温かい口調",
            2,
            5,
            false
    ),

    FORMAL(
            "공식적인",
            "격식 있고 정중한 어조",
            "Formal, polite tone",
            "正式で丁寧な口調",
            5,
            2,
            false
    ),

    HUMOROUS(
            "유머러스한",
            "적절한 가벼운 유머",
            "Light, appropriate humor",
            "適切な軽いユーモア",
            2,
            4,
            true
    ),

    MOTIVATIONAL(
            "동기부여형",
            "행동 유도, 긍정적 결과 강조",
            "Encourage action, highlight positive outcomes",
            "行動を促し、前向きな結果を強調",
            3,
            5,
            false
    ),

    CASUAL(
            "일상적인",
            "자연스러운 일상 대화체",
            "Natural, conversational tone",
            "自然な会話調",
            1,
            4,
            false
    ),

    PROFESSIONAL(
            "전문적인",
            "전문적이고 정확한 어조",
            "Professional, precise tone",
            "専門的で正確な口調",
            4,
            2,
            false
    ),

    EMPATHETIC(
            "공감하는",
            "사용자 감정/상황 이해",
            "Acknowledge user's feelings and situation",
            "利用者の感情・状況を理解",
            3,
            5,
            false
    ),

    POSITIVE(
            "긍정적인",
            "낙관적이고 긍정적인 어조",
            "Optimistic, positive tone",
            "前向きでポジティブな口調",
            3,
            4,
            false
    ),

    INSPIRATIONAL(
            "영감을 주는",
            "창의성과 새 아이디어 자극",
            "Stimulate creativity and new ideas",
            "創造性と新しい発想を刺激",
            3,
            5,
            false
    ),

    NEUTRAL(
            "중립적인",
            "객관적이고 중립적인 어조",
            "Objective, neutral tone",
            "客観的で中立な口調",
            3,
            2,
            false
    ),

    ENTHUSIASTIC(
            "열정적인",
            "에너지 넘치고 열정적인 어조",
            "Energetic, enthusiastic tone",
            "エネルギッシュで熱意のある口調",
            3,
            5,
            false
    ),

    SARCASTIC(
            "비꼬는",
            "빈정대고 풍자적인 어조",
            "Sarcastic, satirical tone",
            "皮肉で風刺的な口調",
            3,
            1,
            true
    ),

    NEGATIVE(
            "부정적인",
            "비판적이고 회의적인 어조",
            "Critical, skeptical tone",
            "批判的で懐疑的な口調",
            3,
            1,
            true
    );

    /** UI 표시용 */
    private final String displayName;

    /** Tone guideline per language */
    private final String guidelineKo;
    private final String guidelineEn;
    private final String guidelineJa;

    /** 형식성 레벨 (1: 매우 캐주얼 ~ 5: 매우 포멀) */
    private final int formalityLevel;
    /** 따뜻함/정서적 친밀감 레벨 (1: 차가움 ~ 5: 매우 따뜻함) */
    private final int warmthLevel;
    /** 민감한 주제에서 사용 시 리스크가 높은 톤인지 여부 */
    private final boolean riskyForSensitiveTopics;

    ToneType(
            String displayName,
            String guidelineKo,
            String guidelineEn,
            String guidelineJa,
            int formalityLevel,
            int warmthLevel,
            boolean riskyForSensitiveTopics
    ) {
        this.displayName = displayName;
        this.guidelineKo = guidelineKo;
        this.guidelineEn = guidelineEn;
        this.guidelineJa = guidelineJa;
        this.formalityLevel = formalityLevel;
        this.warmthLevel = warmthLevel;
        this.riskyForSensitiveTopics = riskyForSensitiveTopics;
    }

    /**
     * 언어 타입에 따라 적절한 톤 가이드라인을 반환한다.
     */
    public String getGuidelineByLang(LanguageType lang) {
        return getByLang(lang, guidelineKo, guidelineEn, guidelineJa);
    }
}
