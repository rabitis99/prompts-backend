package org.example.sharedprompts.domain.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {
    COMMENT,    // 댓글 알림
    LIKE,       // 좋아요 알림
    FAVORITE,   // 즐겨찾기 알림
    FOLLOW      // 팔로우 알림
}


