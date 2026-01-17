package org.example.sharedprompts.domain.notification.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.notification.enums.NotificationCategory;
import org.example.sharedprompts.domain.notification.enums.NotificationPriority;
import org.example.sharedprompts.domain.notification.enums.NotificationType;

import java.io.Serializable;

/**
 * RabbitMQ를 통해 전송되는 알림 메시지 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 알림을 받을 사용자 ID
     */
    @NotNull(message = "사용자 ID는 필수입니다")
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 알림 타입 (COMMENT, LIKE 등)
     */
    @NotNull(message = "알림 타입은 필수입니다")
    @JsonProperty("type")
    private NotificationType type;

    /**
     * 관련 엔티티 ID (프롬프트 ID 또는 댓글 ID)
     */
    @NotNull(message = "관련 엔티티 ID는 필수입니다")
    @JsonProperty("related_entity_id")
    private Long relatedEntityId;

    /**
     * 행동을 수행한 사용자 ID (actor)
     */
    @NotNull(message = "행동자 ID는 필수입니다")
    @JsonProperty("actor_id")
    private Long actorId;

    /**
     * 알림 메시지 내용
     */
    @NotNull(message = "알림 메시지는 필수입니다")
    @JsonProperty("message")
    private String message;

    /**
     * 알림 우선순위
     */
    @JsonProperty("priority")
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    /**
     * 알림 카테고리
     */
    @JsonProperty("category")
    @Builder.Default
    private NotificationCategory category = NotificationCategory.SOCIAL;

    /**
     * 그룹핑 키 (같은 그룹의 알림을 묶을 때 사용)
     */
    @JsonProperty("group_key")
    private String groupKey;
}

