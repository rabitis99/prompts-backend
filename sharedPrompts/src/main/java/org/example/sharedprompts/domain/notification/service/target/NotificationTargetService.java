package org.example.sharedprompts.domain.notification.service.target;

import java.util.List;

/**
 * 알림 대상 결정을 담당하는 Service.
 *
 * - Repository는 관계 조회만 담당하고,
 * - 이 서비스에서 FOLLOWING + NOT BLOCKED 등의 정책을 조합한다.
 */
public interface NotificationTargetService {

    /**
     * 프롬프트 생성 시 SSE 알림을 받을 대상 사용자 ID 목록을 반환한다.
     *
     * @param authorId 프롬프트 작성자 ID
     * @return 알림 대상 사용자 ID 목록
     */
    List<Long> getPromptCreatedTargets(Long authorId);
}


