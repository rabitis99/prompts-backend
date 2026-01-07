package org.example.sharedprompts.domain.prompt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StyleType {

    NARRATIVE(
            "서사형",
            "Present the response in a narrative format, focusing on events or experiences.",
            "이야기 형식으로 전개하며 사건이나 경험을 중심으로 서술합니다.",
            "物語形式で展開し、出来事や体験を中心に説明してください。"
    ),

    BULLET(
            "글머리형",
            "Present key points in concise bullet points.",
            "핵심 포인트를 간결한 글머리 목록으로 정리합니다.",
            "要点を簡潔な箇条書きでまとめてください。"
    ),

    CONCISE(
            "간결형",
            "Deliver only the essential information concisely without unnecessary details.",
            "불필요한 설명을 제거하고 핵심 정보만 간결하게 전달합니다.",
            "不要な説明を省き、要点のみを簡潔に伝えてください。"
    ),

    FORMATTED(
            "서식형",
            "Follow a clear and consistent structure or predefined format.",
            "명확한 구조나 정해진 서식을 따라 작성합니다.",
            "明確な構造や定められたフォーマットに従ってください。"
    ),

    DESCRIPTIVE(
            "묘사형",
            "Provide detailed and vivid descriptions emphasizing sensory details.",
            "감각적이고 시각적인 요소를 강조한 상세한 설명을 제공합니다.",
            "感覚的・視覚的な要素を強調した詳細な描写を行ってください。"
    ),

    INSTRUCTIVE(
            "설명/가이드형",
            "Explain procedures or tasks step by step with clear instructions.",
            "작업이나 과정을 단계별로 명확하게 안내합니다.",
            "作業や手順を段階的に分かりやすく説明してください。"
    ),

    QUESTION_ANSWER(
            "질문-답변형",
            "Present the information in a question-and-answer format.",
            "질문과 답변 형식으로 정보를 전달합니다.",
            "質問と回答の形式で情報を提供してください。"
    ),

    STORYTELLING(
            "스토리텔링형",
            "Use storytelling techniques to convey information and evoke emotions.",
            "스토리텔링 기법으로 정보를 전달하고 감정을 이끌어냅니다.",
            "ストーリーテリングを用いて情報を伝え、感情に訴えてください。"
    ),

    DIALOGUE(
            "대화형",
            "Present the content as a dialogue between two or more parties.",
            "두 사람 이상의 대화 형식으로 내용을 전개합니다.",
            "二人以上の会話形式で内容を展開してください。"
    ),

    COMPARATIVE(
            "비교형",
            "Compare two or more subjects, highlighting similarities and differences.",
            "두 가지 이상의 대상을 비교하여 차이점과 공통점을 강조합니다.",
            "複数の対象を比較し、違いや共通点を明確にしてください。"
    ),

    ANALYTICAL(
            "분석형",
            "Analyze the topic deeply from multiple perspectives.",
            "주제를 다양한 관점에서 깊이 있게 분석합니다.",
            "複数の視点から深く分析してください。"
    ),

    CREATIVE(
            "창의형",
            "Provide creative and original ideas or approaches.",
            "독창적이고 새로운 아이디어나 접근 방식을 제시합니다.",
            "独創的で新しいアイデアやアプローチを提示してください。"
    ),

    TECHNICAL(
            "기술형",
            "Use precise technical language and provide detailed technical explanations.",
            "정확한 기술 용어를 사용하여 상세한 기술 설명을 제공합니다.",
            "正確な技術用語を用いて詳細な技術説明を行ってください。"
    ),

    DETAILED(
            "상세형",
            "Provide thorough and detailed explanations to ensure deep understanding.",
            "깊이 있는 이해를 돕기 위해 상세하고 구체적으로 설명합니다.",
            "深い理解を得られるよう、詳細かつ具体的に説明してください。"
    );

    /** UI 표시용 */
    private final String displayName;

    /** Style guideline per language */
    private final String guidelineEn;
    private final String guidelineKo;
    private final String guidelineJa;
}

