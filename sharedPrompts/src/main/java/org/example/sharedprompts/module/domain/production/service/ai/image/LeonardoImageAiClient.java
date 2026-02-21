package org.example.sharedprompts.module.domain.production.service.ai.image;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.ai.config.properties.LeonardoProperties;
import org.example.sharedprompts.module.domain.production.service.ai.exception.AiClientException;
import org.example.sharedprompts.module.domain.production.service.ai.retry.AiPollingScheduler;
import org.example.sharedprompts.module.domain.production.service.ai.retry.RetryExecutor;
import org.example.sharedprompts.module.domain.production.service.ai.retry.RetryPolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

/**
 * Leonardo Image AI 클라이언트 구현
 * 이미지 생성 및 상태 폴링 지원
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "ai.provider.leonardo.enabled", havingValue = "true")
public class LeonardoImageAiClient implements ImageAIClient {
    
    private static final String GENERATION_ENDPOINT = "/generations";
    private static final String GENERATION_STATUS_ENDPOINT = "/generations/{generationId}";
    
    private final LeonardoProperties properties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final RetryPolicy retryPolicy;
    private final RetryExecutor retryExecutor;
    private final AiPollingScheduler pollingScheduler;
    
    private volatile WebClient webClient;
    
    public LeonardoImageAiClient(
            LeonardoProperties properties,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Qualifier("leonardoRetryPolicy") RetryPolicy retryPolicy,
            RetryExecutor retryExecutor,
            AiPollingScheduler pollingScheduler) {
        this.properties = properties;
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
        this.retryPolicy = retryPolicy;
        this.retryExecutor = retryExecutor;
        this.pollingScheduler = pollingScheduler;
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
                            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
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
    public String generateImage(String prompt, int width, int height, String modelName) {
        return generateImage(prompt, width, height, modelName, null);
    }
    
    /**
     * 이미지 생성 (negative prompt 지원)
     */
    public String generateImage(String prompt, int width, int height, String modelName, String negativePrompt) {
        try {
            // 입력 검증
            if (prompt == null || prompt.isBlank()) {
                throw new AiClientException("Prompt cannot be null or empty");
            }
            if (width <= 0 || height <= 0) {
                throw new AiClientException(String.format("Invalid image dimensions: %dx%d", width, height));
            }
            
            String modelId = modelName != null && !modelName.isBlank() 
                    ? modelName 
                    : properties.getDefaultModelId();
            
            if (modelId == null || modelId.isBlank()) {
                throw new AiClientException("Model ID cannot be null or empty");
            }
            
            log.info("Generating image with Leonardo - modelId: {}, size: {}x{}, prompt length: {}", 
                    modelId, width, height, prompt.length());
            
            // 이미지 생성 요청
            // 가이드에 따른 필드 구성: alchemy, contrast, styleUUID, ultra는 선택적 필드
            LeonardoGenerationRequest request = new LeonardoGenerationRequest(
                    modelId,
                    prompt,
                    negativePrompt,
                    width,
                    height,
                    1,  // num_images: 기본값 1
                    properties.getAlchemy(),  // alchemy: 기본값 false
                    properties.getContrast(),  // contrast: 선택적
                    properties.getStyleUUID(),  // styleUUID: 선택적
                    properties.getUltra()  // ultra: 기본값 false
            );
            
            LeonardoGenerationResponse generationResponse = retryExecutor.executeWithRetry(
                    () -> createGeneration(request),
                    retryPolicy,
                    "Leonardo API call (create generation)"
            );
            
            if (generationResponse == null || generationResponse.getSdGenerationJob() == null) {
                throw new AiClientException("Leonardo API returned empty generation response");
            }
            
            String generationId = generationResponse.getSdGenerationJob().getGenerationId();
            if (generationId == null || generationId.isBlank()) {
                throw new AiClientException("Generation ID is missing");
            }
            
            log.debug("Image generation started - generationId: {}", generationId);
            
            // 상태 폴링
            String imageUrl = pollGenerationStatus(generationId);
            
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new AiClientException("Image URL is empty");
            }
            
            log.debug("Image generation completed - url: {}", imageUrl);
            return imageUrl;
            
        } catch (Exception e) {
            if (e instanceof AiClientException) {
                throw e;
            }
            log.error("Unexpected error during image generation", e);
            throw new AiClientException("Failed to generate image", e);
        }
    }
    
    /**
     * 이미지 생성 요청
     */
    private LeonardoGenerationResponse createGeneration(LeonardoGenerationRequest request) {
        try {
            // 요청 로깅 (PII 보호: 프롬프트는 마스킹)
            log.debug("Leonardo API request - modelId: {}, promptLength: {}, width: {}, height: {}", 
                    request.getModelId(), 
                    request.getPrompt() != null ? request.getPrompt().length() : 0, 
                    request.getWidth(), request.getHeight());
            
            // P1-1: WebClient.block()은 calling thread를 블로킹함
            // RabbitMQ consumer 스레드에서 실행되므로 스레드 점유 문제 발생 가능
            // TODO: 장기적으로 reactive pipeline 전환 (P2-1)
            // 현재는 consumer의 prefetchCount를 AI executor의 thread pool 크기에 맞게 조정 필요
            String responseBody = getWebClient()
                    .post()
                    .uri(GENERATION_ENDPOINT)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .block(); // BLOCKING: Consumer thread is held during API call
            
            if (responseBody == null) {
                throw new AiClientException("Leonardo API returned null response");
            }
            
            return objectMapper.readValue(responseBody, LeonardoGenerationResponse.class);
        } catch (JsonProcessingException e) {
            throw new AiClientException("Failed to parse Leonardo generation response", e);
        } catch (Exception e) {
            // createGeneration은 retryExecutor가 호출하는 작업(operation)이므로,
            // WebClientResponseException 등 모든 비-AiClientException 예외를 AiClientException으로 래핑하여 반환.
            // retryExecutor의 shouldRetry는 원인 체인(getCause())을 순회해 재시도 가능 여부를 판단함.
            if (e instanceof AiClientException) {
                throw e;
            }
            throw new AiClientException("Failed to create generation", e);
        }
    }
    
    /**
     * 생성 상태 폴링
     */
    private String pollGenerationStatus(String generationId) {
        long startTime = System.currentTimeMillis();
        long maxWaitTime = properties.getMaxPollingWaitSeconds() * 1000L;
        int pollInterval = properties.getPollingIntervalSeconds() * 1000;
        
        // 연속 실패 횟수 제한 (executeWithRetry 내부 재시도 후에도 계속 실패하는 경우 조기 중단)
        int maxConsecutiveFailures = properties.getMaxConsecutivePollingFailures();
        int consecutiveFailures = 0;
        
        while (true) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= maxWaitTime) {
                throw new AiClientException(
                        String.format("Image generation timeout after %d seconds", 
                                properties.getMaxPollingWaitSeconds()));
            }
            
            try {
                LeonardoGenerationStatusResponse statusResponse = retryExecutor.executeWithRetry(
                        () -> getGenerationStatus(generationId),
                        retryPolicy,
                        "Leonardo API call (get generation status)"
                );
                
                // 성공 시 실패 카운터 리셋
                consecutiveFailures = 0;
                
                if (statusResponse == null || statusResponse.getGenerationsByPk() == null) {
                    throw new AiClientException("Invalid status response");
                }
                
                LeonardoGenerationStatus status = statusResponse.getGenerationsByPk();
                String statusValue = status.getStatus();
                
                if ("COMPLETE".equalsIgnoreCase(statusValue)) {
                    List<LeonardoGenerationImage> images = status.getGeneratedImages();
                    if (images != null && !images.isEmpty()) {
                        String url = images.get(0).getUrl();
                        if (url != null && !url.isBlank()) {
                            return url;
                        }
                    }
                    throw new AiClientException("Generation completed but no image URL found");
                }
                
                if ("FAILED".equalsIgnoreCase(statusValue)) {
                    throw new AiClientException("Image generation failed");
                }
                
                log.debug("Generation in progress - status: {}, waiting {}s", 
                        statusValue, properties.getPollingIntervalSeconds());
                
                try {
                    pollingScheduler.scheduleDelay(pollInterval).get();
                } catch (Exception delayEx) {
                    if (delayEx.getCause() instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        throw new AiClientException("Polling interrupted", delayEx);
                    }
                    throw new AiClientException("Polling delay failed", delayEx);
                }
                
            } catch (Exception e) {
                if (e instanceof AiClientException) {
                    throw e;
                }
                
                consecutiveFailures++;
                log.warn("Error polling generation status (consecutive failures: {}/{}), retrying...", 
                        consecutiveFailures, maxConsecutiveFailures, e);
                
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    throw new AiClientException(
                            String.format("Polling failed after %d consecutive failures", 
                                    maxConsecutiveFailures), e);
                }
                
                try {
                    pollingScheduler.scheduleDelay(pollInterval).get();
                } catch (Exception delayEx) {
                    if (delayEx.getCause() instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        throw new AiClientException("Polling interrupted", delayEx);
                    }
                    throw new AiClientException("Polling delay failed", delayEx);
                }
            }
        }
    }
    
    /**
     * 생성 상태 조회
     */
    private LeonardoGenerationStatusResponse getGenerationStatus(String generationId) {
        try {
                // P1-1: WebClient.block()은 calling thread를 블로킹함
                // RabbitMQ consumer 스레드에서 실행되므로 스레드 점유 문제 발생 가능
                String responseBody = getWebClient()
                        .get()
                        .uri(GENERATION_STATUS_ENDPOINT, generationId)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                        .block(); // BLOCKING: Consumer thread is held during API call
            
            if (responseBody == null) {
                throw new AiClientException("Leonardo API returned null status response");
            }
            
            return objectMapper.readValue(responseBody, LeonardoGenerationStatusResponse.class);
        } catch (JsonProcessingException e) {
            throw new AiClientException("Failed to parse Leonardo status response", e);
        } catch (Exception e) {
            if (e instanceof AiClientException) {
                throw e;
            }
            throw new AiClientException("Failed to get generation status", e);
        }
    }
    
    
    /**
     * Leonardo 생성 요청 DTO
     * 
     * 참고: Leonardo.AI API 가이드에 따른 필드 구성
     * https://docs.leonardo.ai/reference/creategeneration
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LeonardoGenerationRequest {
        @JsonProperty("modelId")
        private String modelId;
        
        @JsonProperty("prompt")
        private String prompt;
        
        @JsonProperty("negative_prompt")
        private String negativePrompt;
        
        @JsonProperty("width")
        private Integer width;
        
        @JsonProperty("height")
        private Integer height;
        
        @JsonProperty("num_images")
        private Integer numImages;
        
        @JsonProperty("alchemy")
        private Boolean alchemy;
        
        @JsonProperty("contrast")
        private Double contrast;
        
        @JsonProperty("styleUUID")
        private String styleUUID;
        
        @JsonProperty("ultra")
        private Boolean ultra;
    }
    
    /**
     * Leonardo 생성 응답 DTO
     */
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LeonardoGenerationResponse {
        @JsonProperty("sdGenerationJob")
        private LeonardoGenerationJob sdGenerationJob;
    }
    
    /**
     * Leonardo 생성 작업 DTO
     */
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LeonardoGenerationJob {
        @JsonProperty("generationId")
        private String generationId;
    }
    
    /**
     * Leonardo 생성 상태 응답 DTO
     */
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LeonardoGenerationStatusResponse {
        @JsonProperty("generations_by_pk")
        private LeonardoGenerationStatus generationsByPk;
    }
    
    /**
     * Leonardo 생성 상태 DTO
     */
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LeonardoGenerationStatus {
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("generated_images")
        private List<LeonardoGenerationImage> generatedImages;
    }
    
    /**
     * Leonardo 생성 이미지 DTO
     */
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LeonardoGenerationImage {
        @JsonProperty("url")
        private String url;
    }
}

