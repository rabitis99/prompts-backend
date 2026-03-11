package org.example.sharedprompts.domain.prompt.common.enums.output;

/**
 * 응답의 기본 출력 포맷을 표현한다.
 * <p>구체적인 렌더링(예: Markdown 헤딩 레벨, 코드 블록 언어 선택)은
 * 상위 레이어에서 결정하고, 이 enum은 엔진의 거친 힌트로만 사용한다.</p>
 */
public enum OutputFormat {
    /** 순수 텍스트 (플레인 텍스트 또는 최소한의 서식) */
    PLAIN_TEXT,
    /** Markdown을 기본으로 하는 구조화된 텍스트 */
    MARKDOWN,
    /** 코드 블록 중심 응답 (언어는 별도 힌트로 결정) */
    CODE,
    /** JSON, YAML 등 기계가 읽기 쉬운 구조화 데이터 */
    STRUCTURED_DATA
}
