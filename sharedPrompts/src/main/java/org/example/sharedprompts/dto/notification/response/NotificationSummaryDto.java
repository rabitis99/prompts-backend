package org.example.sharedprompts.dto.notification.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NotificationSummaryDto {

    @JsonProperty("unread_count")
    private long unreadCount;
}

