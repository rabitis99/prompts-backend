package org.example.sharedprompts.domain.prompt.application.service.orchestration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ResolutionSource;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ResolvedDomain;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.springframework.stereotype.Component;

/**
 * 도메인 결정 서비스 (파사드).
 * <p>순수 해석 로직은 {@link DomainResolverPort} 구현체에 위임하고,
 * DTO 변환·로깅만 담당한다. 단일 모델 {@link ResolvedDomain} 사용.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainResolutionService {

    private final DomainResolverPort domainResolver;

    /**
     * ActionType + PromptCategory로 도메인 결정 (헥사고날 어댑터용).
     */
    public TaskDomain resolveDomain(ActionTypeInterface actionType, PromptCategory promptCategory) {
        return resolveDomainInternal(actionType, promptCategory).domain();
    }

    /**
     * Full resolution for action + category (e.g. for guideline builder with context).
     */
    public ResolvedDomain resolveDomainResolved(ActionTypeInterface actionType, PromptCategory promptCategory) {
        return resolveDomainInternal(actionType, promptCategory);
    }

    /**
     * Unified 엔진용 도메인 결정.
     *
     * <p>ActionType이 없으므로 Category 기반으로 DomainResolverPort에 위임한다.
     * 폴백인 경우 intentDomainAffinity가 있으면 해당 도메인을 사용한다.</p>
     */
    public ResolvedDomain resolveForUnified(
            PromptCategory categoryHint,
            TaskDomain intentDomainAffinity
    ) {
        ResolvedDomain resolved = domainResolver.resolveDomainWithFallback(null, categoryHint);
        if (resolved.fallback() && intentDomainAffinity != null) {
            return new ResolvedDomain(intentDomainAffinity, false, ResolutionSource.INTENT_AFFINITY);
        }
        if (resolved.fallback()) {
            log.warn("Unified domain fallback used — category: {}, intentAffinity: {}, source: {}. Consider adding mapping.",
                    categoryHint,
                    intentDomainAffinity,
                    resolved.source());
        }
        return resolved;
    }

    private String actionTypeName(ActionTypeInterface actionType) {
        if (actionType == null) return "null";
        return actionType instanceof Enum<?> e ? e.name() : actionType.getClass().getSimpleName();
    }

    private ResolvedDomain resolveDomainInternal(ActionTypeInterface actionType, PromptCategory promptCategory) {
        ResolvedDomain resolved = domainResolver.resolveDomainWithFallback(actionType, promptCategory);
        if (resolved.fallback()) {
            log.warn("Domain fallback used — ActionType: {}, category: {}, source: {}. Consider adding mapping.",
                    actionTypeName(actionType),
                    promptCategory,
                    resolved.source());
        }
        return resolved;
    }
}

