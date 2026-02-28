package org.example.sharedprompts.domain.prompt.domain.resolution;

import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;

/**
 * TaskDomain 해석용 포트.
 *
 * <p>ActionType + PromptCategory → TaskDomain (및 폴백 여부).
 * 애플리케이션/인프라는 이 추상에 의존하며, 구현은 인프라에서 주입한다 (DIP).
 */
public interface DomainResolverPort {

    /**
     * ActionType + PromptCategory로 TaskDomain을 결정한다.
     */
    TaskDomain resolveDomain(ActionTypeInterface actionType, PromptCategory promptCategory);

    /**
     * 도메인과 폴백 여부를 함께 반환한다.
     */
    ResolvedDomain resolveDomainWithFallback(ActionTypeInterface actionType, PromptCategory promptCategory);
}
