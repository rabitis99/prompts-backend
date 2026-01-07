package org.example.sharedprompts.domain.prompt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExperienceLevel {

    BEGINNER(
            "초급",
            "사용자는 초보자입니다. 전문 용어를 최소화하고, 기초부터 차근차근 설명하며, 이해를 돕는 예시를 충분히 제공합니다.",
            "The user is a beginner. Minimize technical jargon, explain concepts step by step, and provide sufficient examples to aid understanding.",
            "利用者は初心者です。専門用語を最小限に抑え、基礎から段階的に説明し、理解を助ける例を十分に提供してください。"
    ),

    INTERMEDIATE(
            "중급",
            "사용자는 중급 수준입니다. 기본 개념은 알고 있다고 가정하고, 중요한 세부사항과 실용적인 팁에 집중합니다.",
            "The user is at an intermediate level. Assume basic knowledge and focus on important details and practical tips.",
            "利用者は中級レベルです。基本的な知識があることを前提に、重要な詳細や実践的なヒントに集中してください。"
    ),

    ADVANCED(
            "고급",
            "사용자는 고급 수준입니다. 심화된 내용, 최적화 기법, 고급 패턴을 포함하여 전문적인 수준의 정보를 제공합니다.",
            "The user is advanced. Include in-depth explanations, optimization techniques, and advanced patterns.",
            "利用者は上級レベルです。高度な内容、最適化手法、高度なパターンを含めて説明してください。"
    ),

    EXPERT(
            "전문가",
            "사용자는 전문가입니다. 최신 연구, 엣지 케이스, 고급 트레이드오프를 논의하며 전문 용어를 자유롭게 사용합니다.",
            "The user is an expert. Discuss edge cases, advanced trade-offs, and recent developments using professional terminology.",
            "利用者は専門家です。最新の動向、エッジケース、高度なトレードオフについて専門用語を用いて議論してください。"
    );

    /** 표시용 (UI, 로그 등) */
    private final String displayName;

    /** Prompt Guideline */
    private final String guidelineKo;
    private final String guidelineEn;
    private final String guidelineJa;
}

