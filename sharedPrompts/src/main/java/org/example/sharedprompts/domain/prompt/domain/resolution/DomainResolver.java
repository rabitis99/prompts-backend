package org.example.sharedprompts.domain.prompt.domain.resolution;

import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;

import java.util.Optional;

/**
 * TaskDomain 결정 전용 도메인 서비스.
 *
 * <p>단일 책임: ActionType + PromptCategory → TaskDomain (및 폴백 여부).
 * Spring 의존 없음. {@code PromptDomainConfig} / {@code ResolutionConfig}에서 생성·주입.
 * {@link DomainResolverPort} 기본 구현체.
 */
public class DomainResolver implements DomainResolverPort {

    /**
     * ActionTypeInterface + PromptCategory로 TaskDomain을 결정한다.
     */
    @Override
    public TaskDomain resolveDomain(ActionTypeInterface actionType, PromptCategory promptCategory) {
        return resolveDomainWithFallback(actionType, promptCategory).domain();
    }

    /**
     * 도메인과 폴백 여부를 함께 반환한다.
     * 폴백: actionType/category가 null이거나 매핑되지 않은 경우 GENERAL로 떨어질 때 true.
     */
    @Override
    public ResolvedDomain resolveDomainWithFallback(ActionTypeInterface actionType, PromptCategory promptCategory) {
        if (actionType == null) {
            return new ResolvedDomain(TaskDomain.GENERAL, true);
        }
        Optional<TaskDomain> fromAction = actionType.getTaskDomain();
        if (fromAction.isPresent() && fromAction.get() != TaskDomain.GENERAL) {
            return new ResolvedDomain(fromAction.get(), false);
        }
        if (promptCategory == null) {
            return new ResolvedDomain(TaskDomain.GENERAL, true);
        }
        TaskDomain fromCategory = promptCategory.getDefaultDomain();
        if (fromCategory != TaskDomain.GENERAL) {
            return new ResolvedDomain(fromCategory, false);
        }
        if (fromAction.isPresent()) {
            return new ResolvedDomain(TaskDomain.GENERAL, false); // 의도적 GENERAL
        }
        return new ResolvedDomain(TaskDomain.GENERAL, true); // 미매핑 폴백
    }
}
