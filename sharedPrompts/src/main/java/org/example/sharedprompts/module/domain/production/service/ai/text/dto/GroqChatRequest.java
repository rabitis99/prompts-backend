package org.example.sharedprompts.module.domain.production.service.ai.text.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Groq Chat API 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroqChatRequest {
    private String model;
    private GroqMessage[] messages;
    
    public GroqChatRequest(String model, GroqMessage message) {
        this.model = model;
        this.messages = new GroqMessage[]{message};
    }
}

