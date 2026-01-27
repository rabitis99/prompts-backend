package org.example.sharedprompts.controller.notification;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.notification.service.core.NotificationService;
import org.example.sharedprompts.domain.notification.service.sse.NotificationSseService;
import org.example.sharedprompts.dto.notification.response.NotificationResponseDto;
import org.example.sharedprompts.dto.notification.response.NotificationSummaryDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSseService sseService;

    /**
     * 알림 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<CustomResponse<Page<NotificationResponseDto>>> getNotifications(
            @CurrentUser AuthUser authUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NotificationResponseDto> notifications = notificationService.getNotifications(authUser.getId(), pageable);
        return CustomResponseHelper.ok(notifications);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<CustomResponse<NotificationSummaryDto>> getUnreadCount(
            @CurrentUser AuthUser authUser
    ) {
        NotificationSummaryDto summary = notificationService.getUnreadCount(authUser.getId());
        return CustomResponseHelper.ok(summary);
    }

    /**
     * 실시간 알림 구독 (SSE)
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@CurrentUser AuthUser authUser) {
        return sseService.subscribe(authUser.getId());
    }

    /**
     * 특정 알림 읽음 처리
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @CurrentUser AuthUser authUser,
            @PathVariable Long id
    ) {
        notificationService.markAsRead(authUser.getId(), id);
        return CustomResponseHelper.noContent();
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@CurrentUser AuthUser authUser) {
        notificationService.markAllAsRead(authUser.getId());
        return CustomResponseHelper.noContent();
    }
}

