package org.example.sharedprompts.module.domain.production.service.ai.text.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Groq Usage DTO
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroqUsage {
    @JsonProperty("prompt_tokens")
    private Long promptTokens;
    
    @JsonProperty("completion_tokens")
    private Long completionTokens;
    
    @JsonProperty("total_tokens")
    private Long totalTokens;
}

