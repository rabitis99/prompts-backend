package org.example.sharedprompts.global.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscordEmbedField {
    private String name;
    private String value;
    private Boolean inline;
}

