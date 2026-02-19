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
     * @return 적합한 PresignedStrategy, 없으면 기본 전략 반환
     * @throws IllegalStateException 전략이 없을 때 발생
     */
    public PresignedStrategy resolve(String contentType) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(contentType))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("No PresignedStrategy found for contentType: {}, using default", contentType);
                    if (strategies.isEmpty()) {
                        throw new IllegalStateException("No PresignedStrategy implementations found");
                    }
                    // 기본 전략: 첫 번째 전략 사용 (일반적으로 DocumentPresignedStrategy)
                    return strategies.get(0);
                });
    }
}

