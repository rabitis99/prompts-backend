package org.example.sharedprompts.domain.user.repository;

import java.time.LocalDate;

/**
 * 일별 사용자 수 Projection 인터페이스
 * Spring Data JPA의 인터페이스 기반 Projection으로 타입 안전한 쿼리 결과 매핑 제공
 */
public interface DailyUserCountProjection {
    /**
     * 날짜 조회
     * 
     * @return 날짜 (LocalDate)
     */
    LocalDate getDate();

    /**
     * 사용자 수 조회
     * 
     * @return 사용자 수
     */
    Long getCount();
}

