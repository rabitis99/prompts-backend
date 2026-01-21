package org.example.sharedprompts.domain.admin.service;

import org.example.sharedprompts.dto.admin.response.RebuildLikeCountsStatusResponseDto;

public interface AdminMaintenanceService {

    /**
     * DB에 저장된 like_count(프롬프트/댓글)를 기준으로
     * Redis의 좋아요 카운트를 재설정한다.
     * - Redis 초기화 이후 관리자에 의해 수동으로 호출되는 것을 전제로 한다.
     */
    void rebuildLikeCountsFromDb();

    /**
     * 좋아요 카운트 재빌드 작업을 비동기적으로 실행한다.
     * HTTP 타임아웃을 피하기 위해 컨트롤러에서는 이 메서드를 사용한다.
     */
    void rebuildLikeCountsFromDbAsync();

    /**
     * 좋아요 카운트 재빌드 작업의 현재 상태를 조회한다.
     */
    RebuildLikeCountsStatusResponseDto getRebuildLikeCountsStatus();
}



