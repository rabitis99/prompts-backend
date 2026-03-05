package org.example.sharedprompts.domain.prompt.common.enums;

/**
 * 프롬프트 엔진 실행 모드.
 *
 * <p>외부 API, 애플리케이션 계층, 도메인 계층에서 공용으로 사용된다.</p>
 */
public enum EngineMode {

    /**
     * 요청 정보를 기반으로 규칙 테이블에 따라
     * V2/V3 해석·생성 전략을 결정한다.
     */
    AUTO,

    /**
     * 품질 우선 V2 파이프라인을 직접 사용한다.
     */
    V2,

    /**
     * Intent/CoreRole/DomainRole 기반 V3 해석 규칙을 사용한다.
     * 출력 생성은 여전히 V2 품질 파이프라인을 통해 수행된다.
     */
    V3
}

