package org.example.sharedprompts.domain.prompt.enums;

import lombok.Getter;

import java.util.List;

import static org.example.sharedprompts.domain.prompt.enums.I18nUtils.getByLang;

@Getter
public enum ToneType {

    FRIENDLY(
            "친근한",
            "친근하고 따뜻한 어조",
            "Friendly, warm tone",
            "親しみやすく温かい口調"
    ),

    FORMAL(
            "공식적인",
            "격식 있고 정중한 어조",
            "Formal, polite tone",
            "正式で丁寧な口調"
    ),

    HUMOROUS(
            "유머러스한",
            "적절한 가벼운 유머",
            "Light, appropriate humor",
            "適切な軽いユーモア"
    ),

    MOTIVATIONAL(
            "동기부여형",
            "행동 유도, 긍정적 결과 강조",
            "Encourage action, highlight positive outcomes",
            "行動を促し、前向きな結果を強調"
    ),

    CASUAL(
            "일상적인",
            "자연스러운 일상 대화체",
            "Natural, conversational tone",
            "自然な会話調"
    ),

    PROFESSIONAL(
            "전문적인",
            "전문적이고 정확한 어조",
            "Professional, precise tone",
            "専門的で正確な口調"
    ),

    EMPATHETIC(
            "공감하는",
            "사용자 감정/상황 이해",
            "Acknowledge user's feelings and situation",
            "利用者の感情・状況を理解"
    ),

    POSITIVE(
            "긍정적인",
            "낙관적이고 긍정적인 어조",
            "Optimistic, positive tone",
            "前向きでポジティブな口調"
    ),

    INSPIRATIONAL(
            "영감을 주는",
            "창의성과 새 아이디어 자극",
            "Stimulate creativity and new ideas",
            "創造性と新しい発想を刺激"
    ),

    NEUTRAL(
            "중립적인",
            "객관적이고 중립적인 어조",
            "Objective, neutral tone",
            "客観的で中立な口調"
    ),

    ENTHUSIASTIC(
            "열정적인",
            "에너지 넘치고 열정적인 어조",
            "Energetic, enthusiastic tone",
            "エネルギッシュで熱意のある口調"
    ),

    SARCASTIC(
            "비꼬는",
            "빈정대고 풍자적인 어조",
            "Sarcastic, satirical tone",
            "皮肉で風刺的な口調"
    ),

    NEGATIVE(
            "부정적인",
            "비판적이고 회의적인 어조",
            "Critical, skeptical tone",
            "批判的で懐疑的な口調"
    );

    /** UI 표시용 */
    private final String displayName;

    /** Tone guideline per language */
    private final String guidelineKo;
    private final String guidelineEn;
    private final String guidelineJa;

    ToneType(String displayName, String guidelineKo, String guidelineEn, String guidelineJa) {
        this.displayName = displayName;
        this.guidelineKo = guidelineKo;
        this.guidelineEn = guidelineEn;
        this.guidelineJa = guidelineJa;
    }

    /**
     * 언어 타입에 따라 적절한 톤 가이드라인을 반환한다.
     */
    public String getGuidelineByLang(LanguageType lang) {
        return getByLang(lang, guidelineKo, guidelineEn, guidelineJa);
    }

    /**
     * 이 ToneType이 추천되는 TaskDomain 목록을 반환한다.
     * <p>여러 도메인에 적합한 톤은 여러 도메인을 반환할 수 있다.</p>
     *
     * @return 추천되는 TaskDomain 목록 (비어있지 않음)
     */
    public List<TaskDomain> getRecommendedDomains() {
        return switch (this) {
            // TECHNICAL 도메인 전용 톤
            case FORMAL ->
                    List.of(TaskDomain.TECHNICAL);
            
            // CREATIVE 도메인 전용 톤
            case INSPIRATIONAL, HUMOROUS, SARCASTIC ->
                    List.of(TaskDomain.CREATIVE);
            
            // PRACTICAL 도메인 전용 톤
            case POSITIVE ->
                    List.of(TaskDomain.PRACTICAL);
            
            // 여러 도메인에 적합한 톤
            case PROFESSIONAL, NEUTRAL ->
                    List.of(TaskDomain.TECHNICAL, TaskDomain.ANALYTICAL);
            case FRIENDLY ->
                    List.of(TaskDomain.EDUCATIONAL, TaskDomain.PRACTICAL, TaskDomain.GENERAL);
            case MOTIVATIONAL ->
                    List.of(TaskDomain.PRACTICAL, TaskDomain.EDUCATIONAL);
            case ENTHUSIASTIC ->
                    List.of(TaskDomain.CREATIVE, TaskDomain.EDUCATIONAL);
            case CASUAL ->
                    List.of(TaskDomain.CREATIVE, TaskDomain.GENERAL);
            case EMPATHETIC ->
                    List.of(TaskDomain.EDUCATIONAL, TaskDomain.PRACTICAL, TaskDomain.GENERAL);
            case NEGATIVE ->
                    List.of(TaskDomain.ANALYTICAL, TaskDomain.PRACTICAL, TaskDomain.GENERAL);
        };
    }
}
