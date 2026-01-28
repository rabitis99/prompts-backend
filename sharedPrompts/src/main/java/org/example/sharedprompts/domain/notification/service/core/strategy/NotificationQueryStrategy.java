package org.example.sharedprompts.domain.notification.service.core.strategy;

import org.example.sharedprompts.dto.notification.response.NotificationResponseDto;
import org.example.sharedprompts.dto.notification.response.NotificationSummaryDto;
import org.example.sharedprompts.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 알림 조회 전략 인터페이스
 * 
 * 다양한 조회 방식(읽지 않은 알림만, 전체 알림, 필터링된 알림 등)을 
 * 전략 패턴으로 구현하여 확장성을 제공합니다.
 */
public interface NotificationQueryStrategy {

    /**
     * 사용자의 알림 목록 조회
     * 
     * @param user 사용자
     * @param pageable 페이징 정보
     * @return 알림 목록
     */
    Page<NotificationResponseDto> getNotifications(User user, Pageable pageable);

    /**
     * 사용자의 읽지 않은 알림 개수 조회
     * 
     * @param user 사용자
     * @return 읽지 않은 알림 개수
     */
    NotificationSummaryDto getUnreadCount(User user);

    /**
     * 전략 타입 반환
     * 
     * @return 전략 타입
     */
    QueryStrategyType getStrategyType();
}

