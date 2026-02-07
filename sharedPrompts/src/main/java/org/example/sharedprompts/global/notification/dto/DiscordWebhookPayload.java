package org.example.sharedprompts.global.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscordWebhookPayload {
    private String content;
    private List<DiscordEmbed> embeds;
}

