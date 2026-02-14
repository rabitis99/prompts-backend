package org.example.sharedprompts.domain.prompt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExperienceLevel {

    BEGINNER(
            "초급",
            "초보자 수준: 전문 용어 최소화, 기초부터 단계별 설명, 충분한 예시 제공",
            "Beginner level: minimize jargon, step-by-step from basics, provide sufficient examples",
            "初心者レベル：専門用語最小限、基礎から段階的説明、十分な例を提供"
    ),

    INTERMEDIATE(
            "중급",
            "중급 수준: 기본 개념 가정, 중요 세부사항과 실용적 팁 집중",
            "Intermediate level: assume basic knowledge, focus on important details and practical tips",
            "中級レベル：基本知識を前提、重要な詳細と実践的ヒントに集中"
    ),

    ADVANCED(
            "고급",
            "고급 수준: 심화 내용, 최적화 기법, 고급 패턴 포함",
            "Advanced level: in-depth content, optimization techniques, advanced patterns",
            "上級レベル：高度な内容、最適化手法、高度なパターンを含む"
    ),

    EXPERT(
            "전문가",
            "전문가 수준: 최신 연구, 엣지 케이스, 고급 트레이드오프, 전문 용어 자유 사용",
            "Expert level: latest research, edge cases, advanced trade-offs, use professional terminology freely",
            "専門家レベル：最新動向、エッジケース、高度なトレードオフ、専門用語を自由に使用"
    );

    /** 표시용 (UI, 로그 등) */
    private final String displayName;

    /** Prompt Guideline */
    private final String guidelineKo;
    private final String guidelineEn;
    private final String guidelineJa;
}

