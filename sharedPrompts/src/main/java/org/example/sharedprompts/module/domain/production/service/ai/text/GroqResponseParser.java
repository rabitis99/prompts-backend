package org.example.sharedprompts.module.domain.production.service.ai.text;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.ai.exception.AiClientException;
import org.example.sharedprompts.module.domain.production.service.ai.text.dto.GroqChatResponse;
import org.example.sharedprompts.module.domain.production.service.ai.text.dto.GroqChoice;
import org.example.sharedprompts.module.domain.production.service.ai.text.dto.GroqMessage;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Groq API 응답 파서
 * JSON 응답과 마크다운/텍스트 응답을 모두 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroqResponseParser {
    
    private final ObjectMapper objectMapper;
    
    /**
     * 응답 본문을 GroqChatResponse로 파싱
     * JSON이 아닌 경우 (마크다운 등) 텍스트로 래핑하여 반환
     */
    public GroqChatResponse parseResponse(String responseBody) {
        if (responseBody == null) {
            throw new AiClientException("Groq API returned null response");
        }
        
        // 응답이 JSON 형식인지 확인
        String trimmedBody = responseBody.trim();
        boolean isJson = (trimmedBody.startsWith("{") && trimmedBody.endsWith("}")) ||
                        (trimmedBody.startsWith("[") && trimmedBody.endsWith("]"));
        
        if (!isJson) {
            // JSON이 아닌 경우 (마크다운 등), 직접 텍스트로 반환하도록 처리
            log.debug("Groq API returned non-JSON response (likely markdown), length: {}", responseBody.length());
            return wrapTextAsResponse(responseBody);
        }
        
        try {
            return objectMapper.readValue(responseBody, GroqChatResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse Groq API response as JSON, treating as plain text. Error: {}", e.getMessage());
            // JSON 파싱 실패 시에도 마크다운처럼 처리
            return wrapTextAsResponse(responseBody);
        }
    }
    
    /**
     * 텍스트 응답을 GroqChatResponse 형태로 래핑
     */
    private GroqChatResponse wrapTextAsResponse(String text) {
        GroqChatResponse response = new GroqChatResponse();
        GroqChoice choice = new GroqChoice();
        GroqMessage message = new GroqMessage();
        message.setContent(text);
        choice.setMessage(message);
        response.setChoices(Collections.singletonList(choice));
        return response;
    }
}

