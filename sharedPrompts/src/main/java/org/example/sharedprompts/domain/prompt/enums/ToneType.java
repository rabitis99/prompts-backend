package org.example.sharedprompts.domain.prompt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ToneType {

    FRIENDLY(
            "친근한",
            "Use a friendly and warm tone that makes the user feel comfortable.",
            "친근하고 따뜻한 어조를 사용하여 사용자가 편안함을 느끼도록 합니다.",
            "親しみやすく温かい口調で、利用者が安心できるようにしてください。"
    ),

    FORMAL(
            "공식적인",
            "Use a formal, polite, and professional tone suitable for official documents.",
            "격식 있고 정중한 표현을 사용하며 공식 문서에 적합한 어조를 유지합니다.",
            "正式で丁寧な表現を用い、公的な文書に適した口調を使用してください。"
    ),

    HUMOROUS(
            "유머러스한",
            "Use light humor where appropriate, ensuring it remains respectful and relevant.",
            "적절한 범위 내에서 가벼운 유머를 사용하되, 무례하거나 부적절하지 않게 합니다.",
            "適切な範囲で軽いユーモアを用い、無礼にならないよう注意してください。"
    ),

    MOTIVATIONAL(
            "동기부여형",
            "Use a motivational tone that encourages action and highlights positive outcomes.",
            "행동을 유도하고 긍정적인 결과를 강조하는 동기부여 어조를 사용합니다.",
            "行動を促し、前向きな結果を強調する口調を使用してください。"
    ),

    CASUAL(
            "일상적인",
            "Use a casual, natural conversational tone without sounding stiff.",
            "딱딱하지 않고 자연스러운 일상 대화체를 사용합니다.",
            "堅苦しくならない自然な会話調で書いてください。"
    ),

    PROFESSIONAL(
            "전문적인",
            "Maintain a professional, precise, and trustworthy tone.",
            "전문 지식과 정확성을 바탕으로 신뢰감 있는 어조를 유지합니다.",
            "専門知識と正確性に基づいた信頼できる口調を使用してください。"
    ),

    EMPATHETIC(
            "공감하는",
            "Use an empathetic tone that acknowledges the user's feelings and situation.",
            "사용자의 감정과 상황을 이해하고 공감하는 어조를 사용합니다.",
            "利用者の感情や状況に寄り添う共感的な口調を使用してください。"
    ),

    SARCASTIC(
            "비꼬는",
            "Use mild sarcasm carefully and only when contextually appropriate.",
            "상황에 맞는 가벼운 풍자를 사용하되 공격적이지 않도록 주의합니다.",
            "文脈に合った軽い皮肉を使用し、攻撃的にならないよう注意してください。"
    ),

    POSITIVE(
            "긍정적인",
            "Maintain an optimistic and positive tone throughout the response.",
            "전체적으로 낙관적이고 긍정적인 어조를 유지합니다.",
            "全体を通して前向きでポジティブな口調を維持してください。"
    ),

    NEGATIVE(
            "부정적인",
            "Clearly communicate problems or risks without unnecessary harshness.",
            "문제점이나 위험 요소를 명확히 전달하되 과도하게 공격적이지 않게 합니다.",
            "問題点やリスクを明確に伝えつつ、過度に攻撃的にならないようにしてください。"
    ),

    INSPIRATIONAL(
            "영감을 주는",
            "Use an inspiring tone that stimulates creativity and new ideas.",
            "창의성과 새로운 아이디어를 자극하는 영감을 주는 어조를 사용합니다.",
            "創造性や新しい発想を刺激するインスピレーションのある口調を使用してください。"
    ),

    NEUTRAL(
            "중립적인",
            "Use an objective and neutral tone without emotional bias.",
            "감정 개입 없이 객관적이고 중립적인 어조를 유지합니다.",
            "感情を排し、客観的で中立な口調を使用してください。"
    ),

    ENTHUSIASTIC(
            "열정적인",
            "Use an energetic and enthusiastic tone that motivates the user.",
            "에너지 넘치고 열정적인 어조로 사용자를 격려합니다.",
            "エネルギッシュで熱意のある口調で利用者を鼓舞してください。"
    );

    /** UI 표시용 */
    private final String displayName;

    /** Tone guideline per language */
    private final String guidelineEn;
    private final String guidelineKo;
    private final String guidelineJa;
}
