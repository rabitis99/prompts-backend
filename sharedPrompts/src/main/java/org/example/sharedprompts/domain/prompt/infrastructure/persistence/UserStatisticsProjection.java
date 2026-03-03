package org.example.sharedprompts.domain.prompt.infrastructure.persistence;

/**
 * 사용자 통계 Projection 인터페이스
 * Spring Data JPA의 인터페이스 기반 Projection으로 타입 안전한 쿼리 결과 매핑 제공
 */
public interface UserStatisticsProjection {
    /**
     * 사용자가 작성한 프롬프트 수 조회
     */
    Long getPromptCount();

    /**
     * 사용자의 프롬프트들에 받은 총 좋아요 수 조회
     */
    Long getTotalLikeCount();
}
