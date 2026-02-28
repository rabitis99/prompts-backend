package org.example.sharedprompts.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.domain.resolution.DomainResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolution.ResolvedDomain;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.guideline.DomainResolution;
import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.springframework.stereotype.Component;

/**
 * 도메인 결정 서비스 (파사드).
 * <p>순수 해석 로직은 {@link DomainResolverPort} 구현체에 위임하고,
 * DTO 변환·로깅·{@link DomainResolution} 래핑만 담당한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainResolutionService {

    private final DomainResolverPort domainResolver;

    /**
     * 도메인 결정 우선순위는 {@link DomainResolverPort#resolveDomainWithFallback}과 동일.
     *
     * @param request 입력 요청 DTO
     * @return 도메인 해결 결과 (도메인 + 폴백 여부)
     */
    public DomainResolution resolveDomain(InputRequestDto request) {
        return resolveDomainInternal(request.getActionType(), request.getPromptCategory());
    }

    /**
     * 도메인만 반환하는 간단한 버전 (PromptGenerator용)
     */
    public TaskDomain resolveDomainSimple(InputRequestDto request) {
        return resolveDomain(request).domain();
    }

    /**
     * ActionType + PromptCategory로 도메인 결정 (헥사고날 어댑터용).
     */
    public TaskDomain resolveDomain(ActionTypeInterface actionType, PromptCategory promptCategory) {
        return resolveDomainInternal(actionType, promptCategory).domain();
    }

    private DomainResolution resolveDomainInternal(ActionTypeInterface actionType, PromptCategory promptCategory) {
        ResolvedDomain resolved = domainResolver.resolveDomainWithFallback(actionType, promptCategory);
        if (resolved.isFallback()) {
            log.warn("Domain fallback used — ActionType: {}, category: {}. Consider adding mapping.",
                    actionType instanceof Enum<?> e ? e.name() : actionType != null ? actionType.getClass().getSimpleName() : "null",
                    promptCategory);
        }
        return new DomainResolution(resolved.domain(), resolved.isFallback());
    }
}

