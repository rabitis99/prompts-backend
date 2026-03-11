package org.example.sharedprompts.domain.prompt.application.engine.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ResolutionSource;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ResolvedDomain;
import org.springframework.stereotype.Component;

/** 도메인 해석 파사드 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainResolutionService {

    private final DomainResolverPort domainResolver;

    public TaskDomain resolveDomain(ActionTypeInterface actionType, PromptCategory promptCategory) {
        return resolveDomainInternal(actionType, promptCategory).domain();
    }

    public ResolvedDomain resolveDomainResolved(ActionTypeInterface actionType, PromptCategory promptCategory) {
        return resolveDomainInternal(actionType, promptCategory);
    }

    public ResolvedDomain resolveForUnified(
            PromptCategory categoryHint,
            TaskDomain intentDomainAffinity
    ) {
        ResolvedDomain resolved = domainResolver.resolveDomainWithFallback(null, categoryHint);

        if (resolved.fallback()) {
            log.warn("Unified domain fallback used — category: {}, intentAffinity: {}, source: {}. Consider adding mapping.",
                    categoryHint,
                    intentDomainAffinity,
                    resolved.source());
            if (intentDomainAffinity != null) {
                return new ResolvedDomain(intentDomainAffinity, true, ResolutionSource.INTENT_AFFINITY);
            }
        }

        return resolved;
    }

    private String actionTypeName(ActionTypeInterface actionType) {
        if (actionType == null) {
            return "null";
        }
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