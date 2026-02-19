package org.example.sharedprompts.module.domain.production.service.ai.image;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentResult;
import org.example.sharedprompts.module.domain.production.service.ai.AIService;
import org.example.sharedprompts.module.domain.production.service.ai.ContentType;
import org.example.sharedprompts.module.domain.production.service.ai.exception.AiClientException;
import org.example.sharedprompts.module.domain.production.service.ai.prompt.ImagePromptBuilder;
import org.example.sharedprompts.module.domain.production.service.ai.strategy.AIModelStrategy;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategy;
import org.example.sharedprompts.module.domain.production.service.storage.StorageStrategyFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Image 콘텐츠 생성 AI 서비스
 * 다양한 이미지 생성 모델 지원
 */
@Service
@Slf4j
public class ImageAIService implements AIService {
    
    private static final int DEFAULT_WIDTH = 1024;
    private static final int DEFAULT_HEIGHT = 1024;
    private static final String DEFAULT_MODEL = "dall-e-3";
    private static final int IMAGE_DOWNLOAD_TIMEOUT_SECONDS = 60;
    private static final String DEFAULT_IMAGE_EXTENSION = ".png";
    private static final String DEFAULT_IMAGE_MIME_TYPE = "image/png";
    
    private final ImageAIClient imageAIClient;
    private final ImagePromptBuilder promptBuilder;
    private final StorageStrategyFactory storageStrategyFactory;
    private final WebClient.Builder webClientBuilder;
    private final AIModelStrategy modelStrategy;
    
    /**
     * 생성자
     * 
     * @param imageAIClient 이미지 AI 클라이언트 (필수)
     * @param promptBuilder 프롬프트 빌더 (필수)
     * @param storageStrategyFactory 스토리지 전략 팩토리 (필수)
     * @param webClientBuilder WebClient 빌더 (필수)
     * @param modelStrategy 모델 전략 (선택적, null 가능)
     */
    public ImageAIService(
            ImageAIClient imageAIClient,
            ImagePromptBuilder promptBuilder,
            StorageStrategyFactory storageStrategyFactory,
            WebClient.Builder webClientBuilder,
            @Qualifier("leonardoModelStrategy") @Nullable AIModelStrategy modelStrategy) {
        this.imageAIClient = imageAIClient;
        this.promptBuilder = promptBuilder;
        this.storageStrategyFactory = storageStrategyFactory;
        this.webClientBuilder = webClientBuilder;
        this.modelStrategy = modelStrategy;
    }
    
    private volatile WebClient imageDownloadClient;
    
    @Override
    public AIContentResult generateContent(AIContentRequest request) {
        try {
            // PII 보호: 프롬프트 전체를 로그에 남기지 않고 길이와 일부만 표시
            String promptPreview = maskPrompt(request.getPrompt());
            log.info("Image AI generation started - prompt: {}, size: {}x{}, model: {}", 
                    promptPreview, request.getWidth(), request.getHeight(), request.getModelName());
            
            // 프롬프트 빌더를 사용하여 최종 프롬프트 생성
            String combinedPrompt = promptBuilder.build(request);
            
            // 모델 이름 결정 (요청에 없으면 전략에서 가져옴, 없으면 기본값)
            String modelName = determineModelName(request);
            
            // 이미지 크기 결정
            int width = request.getWidth() != null ? request.getWidth() : DEFAULT_WIDTH;
            int height = request.getHeight() != null ? request.getHeight() : DEFAULT_HEIGHT;
            
            // AI 클라이언트를 통한 이미지 생성
            String imageUrl = imageAIClient.generateImage(
                    combinedPrompt,
                    width,
                    height,
                    modelName
            );
            
            log.info("Image AI generation completed - url: {}", imageUrl);
            
            // 이미지 URL을 다운로드하여 S3에 저장
            String storedPath = null;
            if (request.getUserId() != null && request.getJobId() != null) {
                try {
                    storedPath = downloadAndStoreImage(imageUrl, request.getUserId(), request.getJobId());
                    log.info("Image downloaded and stored to S3 - path: {}", storedPath);
                } catch (Exception e) {
                    log.warn("Failed to download and store image to S3, using original URL. Error: {}", e.getMessage());
                    // S3 저장 실패 시 원본 URL 사용
                }
            }
            
            // S3에 저장된 경로가 있으면 사용, 없으면 원본 URL 사용
            String finalContent = storedPath != null ? storedPath : imageUrl;
            
            return AIContentResult.success(
                    ContentType.IMAGE,
                    finalContent,
                    modelName
            );
            
        } catch (RuntimeException e) {
            // RuntimeException (AiClientException 포함)은 일시적 오류로 간주하여 예외로 전파 (retry 가능하도록)
            log.warn("Image AI generation failed with transient error, will retry", e);
            throw e;
        } catch (Exception e) {
            // 체크 예외는 영구적 오류로 간주하여 AIContentResult.failure()로 반환
            log.error("Image AI generation failed with permanent error", e);
            return AIContentResult.failure("Image generation failed: " + e.getMessage());
        }
    }
    
    @Override
    public boolean supports(ContentType contentType) {
        return contentType == ContentType.IMAGE;
    }
    
    @Override
    public ContentType getSupportedContentType() {
        return ContentType.IMAGE;
    }
    
    @Override
    public String getModelName() {
        return determineModelName(null);
    }
    
    /**
     * 모델 이름 결정
     * 요청에 모델 이름이 있으면 사용, 없으면 전략에서 가져오고, 그것도 없으면 기본값 사용
     */
    private String determineModelName(AIContentRequest request) {
        if (request != null && request.getModelName() != null && !request.getModelName().isBlank()) {
            return request.getModelName();
        }
        return Optional.ofNullable(modelStrategy)
                .map(AIModelStrategy::getModelName)
                .orElse(DEFAULT_MODEL);
    }
    
    /**
     * 이미지 다운로드용 WebClient 초기화 (Thread-safe 지연 초기화)
     * 큰 이미지 파일을 다운로드하기 위해 버퍼 크기를 10MB로 설정
     */
    private WebClient getImageDownloadClient() {
        WebClient client = imageDownloadClient;
        if (client == null) {
            synchronized (this) {
                client = imageDownloadClient;
                if (client == null) {
                    // ExchangeStrategies를 사용하여 버퍼 크기 설정 (10MB)
                    ExchangeStrategies strategies = ExchangeStrategies.builder()
                            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                            .build();
                    
                    client = webClientBuilder
                            .exchangeStrategies(strategies)
                            .build();
                    imageDownloadClient = client;
                }
            }
        }
        return client;
    }
    
    /**
     * 이미지 URL에서 이미지를 다운로드하여 S3에 저장
     */
    private String downloadAndStoreImage(String imageUrl, Long userId, String jobId) {
        try {
            log.debug("Downloading image from URL: {}", imageUrl);
            
            // 이미지 다운로드 (큰 파일을 위해 별도의 WebClient 사용)
            byte[] imageData = getImageDownloadClient()
                    .get()
                    .uri(imageUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(IMAGE_DOWNLOAD_TIMEOUT_SECONDS))
                    .block();
            
            if (imageData == null || imageData.length == 0) {
                throw new AiClientException("Failed to download image: empty response");
            }
            
            log.debug("Image downloaded - size: {} bytes", imageData.length);
            
            // 파일명 생성 (UUID 사용)
            String fileName = UUID.randomUUID().toString() + DEFAULT_IMAGE_EXTENSION;
            
            // StorageStrategy를 사용하여 S3에 저장
            StorageStrategy storageStrategy = storageStrategyFactory.getStorageStrategy();
            String storedPath = storageStrategy.store(imageData, DEFAULT_IMAGE_MIME_TYPE, userId, jobId, fileName);
            
            log.info("Image stored to S3 successfully - path: {}, size: {} bytes", storedPath, imageData.length);
            return storedPath;
            
        } catch (WebClientResponseException e) {
            log.error("Failed to download image from URL: {} - status: {}", imageUrl, e.getStatusCode());
            throw new AiClientException("Failed to download image: " + e.getStatusCode(), e);
        } catch (Exception e) {
            if (e instanceof AiClientException) {
                throw e;
            }
            log.error("Unexpected error during image download and store", e);
            throw new AiClientException("Failed to download and store image", e);
        }
    }
    
    /**
     * 프롬프트를 마스킹하여 PII 노출을 방지합니다.
     * 프롬프트 길이와 처음 50자만 표시하고 나머지는 마스킹합니다.
     * 
     * @param prompt 원본 프롬프트
     * @return 마스킹된 프롬프트 (예: "[150 chars] Create a beautiful landscape...")
     */
    private String maskPrompt(String prompt) {
        if (prompt == null) {
            return "null";
        }
        if (prompt.isBlank()) {
            return "[empty]";
        }
        
        int length = prompt.length();
        int previewLength = Math.min(50, length);
        String preview = prompt.substring(0, previewLength);
        
        if (length <= previewLength) {
            return String.format("[%d chars] %s", length, preview);
        } else {
            return String.format("[%d chars] %s...", length, preview);
        }
    }
}

