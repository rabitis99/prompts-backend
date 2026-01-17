package org.example.sharedprompts.dto.notification.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.sharedprompts.domain.notification.Notification;
import org.example.sharedprompts.domain.notification.enums.NotificationType;
import org.example.sharedprompts.domain.notification.enums.RelatedEntityType;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NotificationResponseDto {

    private Long id;

    @JsonProperty("type")
    private NotificationType type;

    @JsonProperty("related_entity_type")
    private RelatedEntityType relatedEntityType;

    @JsonProperty("related_entity_id")
    private Long relatedEntityId;

    @JsonProperty("actor_id")
    private Long actorId;

    private String message;

    @JsonProperty("is_read")
    private boolean isRead;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification notification) {
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .actorId(notification.getActorId())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

