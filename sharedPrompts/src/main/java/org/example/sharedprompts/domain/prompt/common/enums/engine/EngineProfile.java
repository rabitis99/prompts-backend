package org.example.sharedprompts.domain.prompt.common.enums.engine;

/**
 * 내부 라우팅/오케스트레이션에서 사용하는 의미 기반 엔진 프로파일.
 *
 * <p>외부 계약은 {@link EngineMode} (AUTO/V2/V3)를 사용하고,
 * 내부에서는 이 프로파일로 구체적인 파이프라인 선택을 표현한다.</p>
 *
 * <p><b>현재 상태:</b> QUALITY_PIPELINE(V2)만 실제 구현됨. FAST_PIPELINE(V3)는 추후 도입 예정이며
 * 요청 시에도 V2로 폴백된다. 자세한 로드맵은 {@link EngineMode} Javadoc 참고.</p>
 */
public enum EngineProfile {

    /**
     * 품질 우선 표준 파이프라인 (V2).
     * Clarify→Solve→Verify→Repair 4단계. 현재 유일하게 구현된 파이프라인.
     */
    QUALITY_PIPELINE,

    /**
     * 경량/저지연 파이프라인 (V3 예정).
     * 아직 미구현으로, 라우팅 결과가 FAST_PIPELINE이어도 실제 실행은 QUALITY_PIPELINE(V2)로 폴백.
     */
    FAST_PIPELINE,

    /**
     * JSON Schema / 포맷 엄격 준수 프로파일.
     *
     * <p>JSON Schema가 존재하거나 구조화 출력이 강제되는 경우 사용된다.
     * 내부적으로는 품질 파이프라인에 스키마/포맷 제약을 더한 의미로 해석한다.</p>
     */
    JSON_STRICT,

    /**
     * 요청/규칙에 따라 위의 프로파일 중 하나로 결정되는 자동 모드.
     *
     * <p>외부 {@link EngineMode#AUTO} 와 매핑되며, 실제 결정된 프로파일은
     * 라우팅 결과 메타데이터로 함께 제공된다.</p>
     */
    AUTO;

    /** 자동 모드 여부 */
    public boolean isAuto() {
        return this == AUTO;
    }
}
