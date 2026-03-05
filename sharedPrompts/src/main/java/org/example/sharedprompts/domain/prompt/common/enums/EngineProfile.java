package org.example.sharedprompts.domain.prompt.common.enums;

/**
 * 내부 라우팅/오케스트레이션에서 사용하는 의미 기반 엔진 프로파일.
 *
 * <p>외부 계약은 여전히 {@link EngineMode} (AUTO/V2/V3)를 사용하고,
 * 내부에서는 보다 의미 있는 프로파일을 사용해 라우팅 결정을 표현한다.</p>
 */
public enum EngineProfile {

    /**
     * 품질 우선 표준 파이프라인.
     * <p>현재는 V2 품질 파이프라인에 매핑된다.</p>
     */
    QUALITY_PIPELINE,

    /**
     * 경량/저지연 파이프라인.
     *
     * <p>기존 V3 또는 향후 Fast 경로를 의미하며,
     * 아직 별도 파이프라인이 구현되지 않아 품질 파이프라인으로 폴백된다.</p>
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

