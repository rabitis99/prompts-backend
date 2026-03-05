package org.example.sharedprompts.domain.prompt.common.enums.action;

/**
 * LLM 응답의 전형적인 구조를 표현한다.
 * <p>실제 토큰 생성 시에는 이 정보를 기반으로
 * "단락 위주", "글머리표 위주", "단계별 절차" 등의 힌트로 사용된다.</p>
 */
public enum ResponseStructure {
    /** 하나 이상의 단락으로 된 자유 서술형 응답 */
    PARAGRAPH,
    /** 글머리표/번호 목록 중심 응답 */
    BULLET_LIST,
    /** 순차적인 단계나 절차를 강조하는 응답 */
    STEP_BY_STEP,
    /** 질문-답변 쌍으로 구성된 응답 */
    QA_PAIRS,
    /** 코드 블록과 짧은 설명이 섞인 응답 */
    CODE_WITH_EXPLANATION,
    /** 표/매트릭스 중심 응답 */
    TABLE,
    /** 대화문 형태의 응답 */
    DIALOGUE,
    /** 위의 어떤 한 가지로 한정되기 어려운 혼합 구조 */
    MIXED
}

