package org.example.sharedprompts.module.domain.production.service.production.presign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Content-Type 기반 PresignedStrategy 해결자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PresignedStrategyResolver {

    private final List<PresignedStrategy> strategies;

    /**
     * Content-Type에 맞는 PresignedStrategy 반환
     * 
     * @param contentType 파일의 Content-Type
     * @return 적합한 PresignedStrategy
     * @throws IllegalArgumentException 지원되지 않는 Content-Type이거나 null일 때 발생
     */
    public PresignedStrategy resolve(String contentType) {
        if (contentType == null) {
            log.warn("ContentType is null");
            throw new IllegalArgumentException("Content type cannot be null");
        }
        return strategies.stream()
                .filter(strategy -> strategy.supports(contentType))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("No PresignedStrategy found for contentType: {}", contentType);
                    return new IllegalArgumentException(
                        "Unsupported content type for presigned URL generation: " + contentType);
                });
    }
}

