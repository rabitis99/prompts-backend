package org.example.sharedprompts.domain.prompt.common.enums;

/**
 * 프롬프트 엔진 실행 모드.
 *
 * <p>외부 API, 애플리케이션 계층, 도메인 계층에서 공용으로 사용된다.</p>
 *
 * <h3>의미 및 로드맵</h3>
 * <ul>
 *   <li><b>AUTO</b>: Intent·규칙에 따라 내부에서 V2 또는 V3(향후) 프로파일로 해석.</li>
 *   <li><b>V2</b>: 품질 우선 파이프라인(Clarify→Solve→Verify→Repair). 현재 유일하게 실제 파이프라인이 구현된 모드.</li>
 *   <li><b>V3</b>: 경량/저지연 경로. 현재는 별도 파이프라인 미구현으로 요청 시에도 내부적으로 V2로 폴백.</li>
 * </ul>
 * <p>정리: 현재는 모든 요청이 V2 품질 파이프라인으로 수렴하며, V3 전용 경로는 추후 도입 예정.</p>
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
     * 현재는 별도 파이프라인 미구현으로 출력 생성은 V2 품질 파이프라인으로 수행된다.
     */
    V3
}

