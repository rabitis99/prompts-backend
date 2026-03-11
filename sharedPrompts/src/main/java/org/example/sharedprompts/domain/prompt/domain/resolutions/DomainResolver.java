package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

import java.util.Optional;

/**
 * TaskDomain 결정 전용 도메인 서비스.
 *
 * <p>단일 책임: ActionType + PromptCategory → TaskDomain (및 폴백 여부).
 * Spring/로깅 의존 없음. 인스턴스는 {@link org.example.sharedprompts.domain.prompt.infrastructure.config.ResolutionConfig}에서 생성.
 */
public class DomainResolver implements DomainResolverPort {

    @Override
    public ResolvedDomain resolveDomain(ActionTypeInterface actionType, PromptCategory promptCategory) {
        return resolveDomainWithFallback(actionType, promptCategory);
    }

    /**
     * 도메인과 폴백 여부를 함께 반환한다.
     * 폴백: actionType/category가 null이거나 매핑되지 않은 경우 GENERAL로 떨어질 때 true.
     */
    @Override
    public ResolvedDomain resolveDomainWithFallback(ActionTypeInterface actionType, PromptCategory promptCategory) {
        if (actionType == null) {
            return new ResolvedDomain(TaskDomain.GENERAL, true, ResolutionSource.FALLBACK);
        }
        Optional<TaskDomain> fromAction = actionType.getTaskDomain();
        if (fromAction.isPresent() && fromAction.get() != TaskDomain.GENERAL) {
            return new ResolvedDomain(fromAction.get(), false, ResolutionSource.ACTION_TYPE);
        }
        if (promptCategory == null) {
            return new ResolvedDomain(TaskDomain.GENERAL, true, ResolutionSource.FALLBACK);
        }
        TaskDomain fromCategory = promptCategory.getDefaultDomain();
        if (fromCategory != TaskDomain.GENERAL) {
            return new ResolvedDomain(fromCategory, false, ResolutionSource.PROMPT_CATEGORY);
        }
        if (fromAction.isPresent()) {
            return new ResolvedDomain(TaskDomain.GENERAL, false, ResolutionSource.DEFAULT); // 의도적 GENERAL
        }
        return new ResolvedDomain(TaskDomain.GENERAL, true, ResolutionSource.FALLBACK); // 미매핑 폴백
    }
}
