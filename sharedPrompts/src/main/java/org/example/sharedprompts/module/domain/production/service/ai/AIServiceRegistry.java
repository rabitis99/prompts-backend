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
 * 
 * <p>서비스 등록 전략:
 * <ul>
 *   <li>동일 ContentType에 대해 첫 번째로 등록된 서비스만 사용 (putIfAbsent 사용)</li>
 *   <li>Spring의 빈 주입 순서에 따라 서비스가 등록되므로, 우선순위가 필요한 경우 @Order 어노테이션 사용 권장</li>
 *   <li>중복 등록 시도는 경고 로그로 기록되지만 덮어쓰지 않음</li>
 * </ul>
 */
@Component
@Slf4j
public class AIServiceRegistry {
    
    private final Map<ContentType, AIService> serviceMap = new ConcurrentHashMap<>();
    
    public AIServiceRegistry(List<AIService> aiServices) {
        // 모든 AIService 구현체를 등록
        // putIfAbsent를 사용하여 첫 번째로 등록된 서비스만 유지 (덮어쓰기 방지)
        for (AIService service : aiServices) {
            service.getSupportedContentType().ifPresent(supportedType -> {
                AIService existing = serviceMap.putIfAbsent(supportedType, service);
                if (existing != null) {
                    log.warn("AIService for ContentType {} already registered: {}. Skipping: {}",
                            supportedType, existing.getClass().getSimpleName(),
                            service.getClass().getSimpleName());
                } else {
                    log.info("Registered AIService: {} for ContentType: {}", 
                            service.getClass().getSimpleName(), supportedType);
                }
            });
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
     * 등록된 모든 서비스 목록 조회
     */
    public List<String> getRegisteredServices() {
        return serviceMap.entrySet().stream()
                .map(entry -> entry.getKey() + " -> " + entry.getValue().getClass().getSimpleName())
                .collect(Collectors.toList());
    }
}


