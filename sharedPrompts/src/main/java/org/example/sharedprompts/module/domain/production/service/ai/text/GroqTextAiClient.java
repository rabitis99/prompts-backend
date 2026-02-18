package org.example.sharedprompts.module.domain.production.service.ai.text;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.ai.config.properties.GroqProperties;
import org.example.sharedprompts.module.domain.production.service.ai.exception.AiClientException;
import org.example.sharedprompts.module.domain.production.service.ai.retry.RetryPolicy;
import org.example.sharedprompts.module.domain.production.service.ai.text.dto.GroqChatRequest;
import org.example.sharedprompts.module.domain.production.service.ai.text.dto.GroqChatResponse;
import org.example.sharedprompts.module.domain.production.service.ai.text.dto.GroqMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Groq Text AI 클라이언트 구현
 * WebClient를 사용한 반응형 HTTP 클라이언트
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "ai.provider.groq.enabled", havingValue = "true")
public class GroqTextAiClient implements TextAiClient {
    
    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
    private static final String ROLE_USER = "user";
    
    private final GroqProperties properties;
    private final WebClient.Builder webClientBuilder;
    private final GroqResponseParser responseParser;
    private final RetryPolicy retryPolicy;
    
    private volatile WebClient webClient;
    
    /**
     * 생성자
     * @Qualifier를 생성자 파라미터에 명시적으로 지정
     */
    public GroqTextAiClient(
            GroqProperties properties,
            WebClient.Builder webClientBuilder,
            GroqResponseParser responseParser,
            @Qualifier("groqRetryPolicy") RetryPolicy retryPolicy) {
        this.properties = properties;
        this.webClientBuilder = webClientBuilder;
        this.responseParser = responseParser;
        this.retryPolicy = retryPolicy;
    }
    
    /**
     * WebClient 초기화 (Thread-safe 지연 초기화)
     */
    private WebClient getWebClient() {
        WebClient client = webClient;
        if (client == null) {
            synchronized (this) {
                client = webClient;
                if (client == null) {
                    client = webClientBuilder
                            .baseUrl(properties.getBaseUrl())
                            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getSecretKey())
                            .build();
                    webClient = client;
                }
            }
        }
        return client;
    }
    
    @Override
    public String generateText(String prompt, String modelName, String contentTypeHint) {
        try {
            String model = modelName != null && !modelName.isBlank() 
                    ? modelName 
                    : properties.getDefaultModel();
            
            log.debug("Generating text with Groq - model: {}, contentTypeHint: {}", model, contentTypeHint);
            
            GroqChatRequest request = new GroqChatRequest(
                    model,
                    new GroqMessage(ROLE_USER, prompt)
            );
            
            GroqChatResponse response = executeWithRetry(request);
            
            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new AiClientException("Groq API returned empty response");
            }
            
            String generatedText = response.getChoices().get(0).getMessage().getContent();
            
            if (generatedText == null || generatedText.isBlank()) {
                throw new AiClientException("Generated text is empty");
            }
            
            log.debug("Text generation completed - length: {}", generatedText.length());
            return generatedText;
            
        } catch (WebClientResponseException e) {
            log.error("Groq API error - status: {}, message: {}", e.getStatusCode(), e.getMessage());
            throw new AiClientException(
                    String.format("Groq API error: %s", e.getStatusCode()), e);
        } catch (Exception e) {
            if (e instanceof AiClientException) {
                throw e;
            }
            log.error("Unexpected error during text generation", e);
            throw new AiClientException("Failed to generate text", e);
        }
    }
    
    /**
     * 재시도 로직을 포함한 API 호출
     */
    private GroqChatResponse executeWithRetry(GroqChatRequest request) {
        int attempt = 1;
        Exception lastException = null;
        
        while (attempt <= retryPolicy.getMaxRetries() + 1) {
            try {
                String responseBody = getWebClient()
                        .post()
                        .uri(CHAT_COMPLETIONS_ENDPOINT)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                        .block();
                
                // 응답 파서를 사용하여 응답 처리
                return responseParser.parseResponse(responseBody);
                
            } catch (Exception e) {
                lastException = e;
                
                if (attempt > retryPolicy.getMaxRetries() || 
                    !retryPolicy.shouldRetry(attempt, e)) {
                    break;
                }
                
                long delayMs = retryPolicy.calculateDelayMs(attempt);
                log.warn("Groq API call failed - attempt: {}/{}, retrying after {}ms. Error: {}", 
                        attempt, retryPolicy.getMaxRetries() + 1, delayMs, e.getMessage());
                
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AiClientException("Retry interrupted", ie);
                }
                
                attempt++;
            }
        }
        
        // 모든 재시도 실패
        throw new AiClientException(
                String.format("Groq API call failed after %d attempts", attempt), 
                lastException);
    }
}

