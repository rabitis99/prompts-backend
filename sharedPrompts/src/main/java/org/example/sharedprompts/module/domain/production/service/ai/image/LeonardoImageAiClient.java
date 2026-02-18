package org.example.sharedprompts.module.domain.production.service.ai.image;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.ai.config.properties.LeonardoProperties;
import org.example.sharedprompts.module.domain.production.service.ai.exception.AiClientException;
import org.example.sharedprompts.module.domain.production.service.ai.retry.RetryPolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
    
    private WebClient webClient;
    
    /**
     * 생성자
     * @Qualifier를 생성자 파라미터에 명시적으로 지정
     */
    public LeonardoImageAiClient(
            LeonardoProperties properties,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Qualifier("leonardoRetryPolicy") RetryPolicy retryPolicy) {
        this.properties = properties;
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
        this.retryPolicy = retryPolicy;
    }
    
    /**
     * WebClient 초기화 (지연 초기화)
     */
    private WebClient getWebClient() {
        if (webClient == null) {
            this.webClient = webClientBuilder
                    .baseUrl(properties.getBaseUrl())
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getSecretKey())
                    .build();
        }
        return webClient;
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
            
            LeonardoGenerationResponse generationResponse = executeWithRetry(() -> 
                    createGeneration(request));
            
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
            
        } catch (WebClientResponseException e) {
            log.error("Leonardo API error - status: {}, message: {}", e.getStatusCode(), e.getMessage());
            throw new AiClientException(
                    String.format("Leonardo API error: %s", e.getStatusCode()), e);
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
            // 요청 로깅
            try {
                String requestJson = objectMapper.writeValueAsString(request);
                log.debug("Leonardo API request: {}", requestJson);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize request for logging", e);
            }
            
            String responseBody = getWebClient()
                    .post()
                    .uri(GENERATION_ENDPOINT)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .block();
            
            if (responseBody == null) {
                throw new AiClientException("Leonardo API returned null response");
            }
            
            return objectMapper.readValue(responseBody, LeonardoGenerationResponse.class);
        } catch (WebClientResponseException e) {
            // 400 Bad Request 등의 클라이언트 오류 시 응답 본문 로깅
            String responseBody = e.getResponseBodyAsString();
            String requestInfo = String.format("modelId: %s, prompt: %s, width: %d, height: %d", 
                    request.getModelId(), request.getPrompt(), request.getWidth(), request.getHeight());
            
            log.error("Leonardo API error - status: {}, request: {}, response: {}", 
                    e.getStatusCode(), requestInfo, responseBody != null ? responseBody : "no response body");
            
            // Content filter 오류인지 확인 (403 FORBIDDEN)
            if (e.getStatusCode().value() == 403 && responseBody != null) {
                String lowerBody = responseBody.toLowerCase();
                if (lowerBody.contains("filter") && 
                    (lowerBody.contains("inappropriate") || 
                     lowerBody.contains("known person") || 
                     lowerBody.contains("blocked"))) {
                    String errorMessage = String.format(
                            "Leonardo API: Content filter blocked the request. " +
                            "The prompt may contain references to a known person or inappropriate content. " +
                            "This error is not retryable. Please modify the prompt and try again. " +
                            "Original error: %s",
                            responseBody);
                    throw new AiClientException(errorMessage, e);
                }
            }
            
            // 모델 지원 오류인지 확인
            if (responseBody != null && responseBody.contains("model is not supported")) {
                String errorMessage = String.format(
                        "Leonardo API: Model ID '%s' is not supported in this API version. " +
                        "Please check the model ID in your configuration (ai.provider.leonardo.default-model-id) " +
                        "or visit https://docs.leonardo.ai/docs/commonly-used-api-values for valid model IDs. " +
                        "Original error: %s",
                        request.getModelId(), responseBody);
                throw new AiClientException(errorMessage, e);
            }
            
            throw new AiClientException(
                    String.format("Leonardo API error: %s - %s", 
                            e.getStatusCode(), 
                            responseBody != null ? responseBody : e.getMessage()), 
                    e);
        } catch (JsonProcessingException e) {
            throw new AiClientException("Failed to parse Leonardo generation response", e);
        } catch (Exception e) {
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
        
        while (true) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= maxWaitTime) {
                throw new AiClientException(
                        String.format("Image generation timeout after %d seconds", 
                                properties.getMaxPollingWaitSeconds()));
            }
            
            try {
                LeonardoGenerationStatusResponse statusResponse = executeWithRetry(() -> 
                        getGenerationStatus(generationId));
                
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
                
                // 진행 중이면 대기 후 재시도
                log.debug("Generation in progress - status: {}, waiting {}s", 
                        statusValue, properties.getPollingIntervalSeconds());
                
                try {
                    Thread.sleep(pollInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AiClientException("Polling interrupted", ie);
                }
                
            } catch (Exception e) {
                if (e instanceof AiClientException) {
                    throw e;
                }
                log.warn("Error polling generation status, retrying...", e);
                try {
                    Thread.sleep(pollInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AiClientException("Polling interrupted", ie);
                }
            }
        }
    }
    
    /**
     * 생성 상태 조회
     */
    private LeonardoGenerationStatusResponse getGenerationStatus(String generationId) {
        try {
            String responseBody = getWebClient()
                    .get()
                    .uri(GENERATION_STATUS_ENDPOINT, generationId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .block();
            
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
     * 재시도 로직을 포함한 API 호출
     */
    private <T> T executeWithRetry(RetryableOperation<T> operation) {
        int attempt = 1;
        Exception lastException = null;
        
        while (attempt <= retryPolicy.getMaxRetries() + 1) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                
                if (attempt > retryPolicy.getMaxRetries() || 
                    !retryPolicy.shouldRetry(attempt, e)) {
                    break;
                }
                
                long delayMs = retryPolicy.calculateDelayMs(attempt);
                log.warn("Leonardo API call failed - attempt: {}/{}, retrying after {}ms. Error: {}", 
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
                String.format("Leonardo API call failed after %d attempts", attempt), 
                lastException);
    }
    
    /**
     * 재시도 가능한 작업 인터페이스
     */
    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws Exception;
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LeonardoGenerationRequest {
        @JsonProperty("modelId")
        private String modelId;
        
        @JsonProperty("prompt")
        private String prompt;
        
        @JsonProperty("negativePrompt")
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

