package org.example.sharedprompts.module.domain.production.service.ai.text.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("temperature")
    private Double temperature;
    
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    /**
     * 단일 메시지를 받는 생성자 (하위 호환성 유지)
     */
    public GroqChatRequest(String model, GroqMessage message) {
        this.model = model;
        this.messages = new GroqMessage[]{message};
    }
    
    /**
     * 시스템 프롬프트와 사용자 프롬프트를 분리하여 받는 생성자
     * Groq Chat Completions API의 권장 방식으로, 시스템 지시사항과 사용자 입력을 명확히 구분합니다.
     * 
     * @param model 모델 이름
     * @param systemPrompt 시스템 프롬프트 (모델의 역할과 행동 지침)
     * @param userPrompt 사용자 프롬프트 (실제 요청 내용)
     */
    public GroqChatRequest(String model, String systemPrompt, String userPrompt) {
        this.model = model;
        this.messages = new GroqMessage[]{
                new GroqMessage("system", systemPrompt),
                new GroqMessage("user", userPrompt)
        };
    }
    
    /**
     * 시스템 프롬프트와 사용자 프롬프트를 분리하여 받는 생성자 (추가 파라미터 포함)
     * 
     * @param model 모델 이름
     * @param systemPrompt 시스템 프롬프트
     * @param userPrompt 사용자 프롬프트
     * @param temperature 생성 온도 (0.0 ~ 2.0, null이면 모델 기본값 사용)
     * @param maxTokens 최대 토큰 수 (null이면 모델 기본값 사용)
     */
    public GroqChatRequest(String model, String systemPrompt, String userPrompt, 
                          Double temperature, Integer maxTokens) {
        this.model = model;
        this.messages = new GroqMessage[]{
                new GroqMessage("system", systemPrompt),
                new GroqMessage("user", userPrompt)
        };
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }
}

