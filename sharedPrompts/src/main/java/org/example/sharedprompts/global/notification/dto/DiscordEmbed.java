package org.example.sharedprompts.global.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscordEmbed {
    private String title;
    private String description;
    private Integer color;
    private List<DiscordEmbedField> fields;
    private DiscordEmbedFooter footer;
    private String timestamp;
}

