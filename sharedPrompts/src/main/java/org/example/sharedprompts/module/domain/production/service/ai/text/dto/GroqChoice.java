package org.example.sharedprompts.module.domain.production.service.ai.text.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Groq Choice DTO
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroqChoice {
    private Integer index;
    private GroqMessage message;
    
    @JsonProperty("finish_reason")
    private String finishReason;
}

