package org.example.sharedprompts.module.domain.production.service.ai.text;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.ai.config.properties.GroqProperties;
import org.example.sharedprompts.module.domain.production.service.ai.exception.AiClientException;
import org.example.sharedprompts.module.domain.production.service.ai.retry.RetryExecutor;
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
    private final RetryExecutor retryExecutor;
    
    private volatile WebClient webClient;
    
    /**
     * 생성자
     * @Qualifier를 생성자 파라미터에 명시적으로 지정
     */
    public GroqTextAiClient(
            GroqProperties properties,
            WebClient.Builder webClientBuilder,
            GroqResponseParser responseParser,
            @Qualifier("groqRetryPolicy") RetryPolicy retryPolicy,
            RetryExecutor retryExecutor) {
        this.properties = properties;
        this.webClientBuilder = webClientBuilder;
        this.responseParser = responseParser;
        this.retryPolicy = retryPolicy;
        this.retryExecutor = retryExecutor;
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
        String model = modelName != null && !modelName.isBlank() 
                ? modelName 
                : properties.getDefaultModelId();
        
        log.debug("Generating text with Groq - model: {}, contentTypeHint: {}", model, contentTypeHint);
        
        // 시스템 프롬프트와 사용자 프롬프트 분리
        // prompt는 TextPromptBuilder에서 이미 시스템 프롬프트와 사용자 프롬프트가 합쳐진 형태이므로
        // 현재는 단일 메시지로 전송 (하위 호환성 유지)
        // 향후 TextPromptBuilder가 분리된 형태를 제공하면 새로운 생성자 사용 가능
        GroqChatRequest request = new GroqChatRequest(
                model,
                new GroqMessage(ROLE_USER, prompt)
        );
        
        // 공통 RetryExecutor를 사용하여 재시도 로직 실행
        // P1-1: WebClient.block()은 calling thread를 블로킹함
        // RabbitMQ consumer 스레드에서 실행되므로 스레드 점유 문제 발생 가능
        // TODO: 장기적으로 reactive pipeline 전환 (P2-1)
        GroqChatResponse response = retryExecutor.executeWithRetry(
                () -> {
                    String responseBody = getWebClient()
                            .post()
                            .uri(CHAT_COMPLETIONS_ENDPOINT)
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                            .block(); // BLOCKING: Consumer thread is held during API call
                    if (responseBody == null) {
                        throw new AiClientException("Groq API returned empty response body (e.g. 204 or empty 2xx)");
                    }
                    // 응답 파서를 사용하여 응답 처리
                    return responseParser.parseResponse(responseBody);
                },
                retryPolicy,
                "Groq API call"
        );
        
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new AiClientException("Groq API returned empty response");
        }
        
        GroqMessage message = response.getChoices().get(0).getMessage();
        if (message == null) {
            throw new AiClientException("Groq API returned choice with null message");
        }
        
        String generatedText = message.getContent();
        
        if (generatedText == null || generatedText.isBlank()) {
            throw new AiClientException("Generated text is empty");
        }
        
        log.debug("Text generation completed - length: {}", generatedText.length());
        return generatedText;
    }
}

