package org.example.sharedprompts.domain.prompt.application.service.orchestration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ResolvedDomain;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.guideline.i18n.DomainResolution;
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

    private String actionTypeName(ActionTypeInterface actionType) {
        if (actionType == null) return "null";
        return actionType instanceof Enum<?> e ? e.name() : actionType.getClass().getSimpleName();
    }

    private DomainResolution resolveDomainInternal(ActionTypeInterface actionType, PromptCategory promptCategory) {
        ResolvedDomain resolved = domainResolver.resolveDomainWithFallback(actionType, promptCategory);
        if (resolved.isFallback()) {
            log.warn("Domain fallback used — ActionType: {}, category: {}. Consider adding mapping.",
                    actionTypeName(actionType),
                    promptCategory);
        }
        return new DomainResolution(resolved.domain(), resolved.isFallback());
    }
}

