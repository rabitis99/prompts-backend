package org.example.sharedprompts.module.domain.production.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.ai.exception.UnsupportedContentTypeException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AIService 레지스트리
 * 여러 AI 서비스를 등록하고 ContentType에 따라 적절한 서비스를 찾아 반환
 * Thread-safe한 ConcurrentHashMap 사용
 */
@Component
@Slf4j
public class AIServiceRegistry {
    
    private final Map<ContentType, AIService> serviceMap = new ConcurrentHashMap<>();
    
    public AIServiceRegistry(List<AIService> aiServices) {
        // 모든 AIService 구현체를 등록
        for (AIService service : aiServices) {
            ContentType supportedType = findSupportedType(service);
            if (supportedType != null) {
                serviceMap.put(supportedType, service);
                log.info("Registered AIService: {} for ContentType: {}", 
                        service.getClass().getSimpleName(), supportedType);
            }
        }
    }
    
    /**
     * ContentType에 맞는 AIService 조회
     */
    public AIService getService(ContentType contentType) {
        AIService service = serviceMap.get(contentType);
        if (service == null) {
            throw new UnsupportedContentTypeException(contentType);
        }
        return service;
    }
    
    /**
     * 서비스가 지원하는 ContentType 찾기
     */
    private ContentType findSupportedType(AIService service) {
        for (ContentType type : ContentType.values()) {
            if (service.supports(type)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 등록된 모든 서비스 목록 조회
     */
    public List<String> getRegisteredServices() {
        return serviceMap.entrySet().stream()
                .map(entry -> entry.getKey() + " -> " + entry.getValue().getClass().getSimpleName())
                .collect(Collectors.toList());
    }
}


