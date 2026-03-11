package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

/**
 * TaskDomain 해석용 도메인 인터페이스.
 *
 * <p>ActionType + PromptCategory → TaskDomain (및 폴백 여부).
 * Application/Infrastructure는 이 추상에만 의존하고, 구현은 Config에서 주입한다 (DIP).
 *
 * <p><b>공개 API.</b> 구현: {@link DomainResolver}.
 */
public interface DomainResolverPort {

    /**
     * ActionType + PromptCategory로 TaskDomain을 결정한다.
     */
    ResolvedDomain resolveDomain(ActionTypeInterface actionType, PromptCategory promptCategory);

    /**
     * 도메인과 폴백 여부를 함께 반환한다.
     */
    ResolvedDomain resolveDomainWithFallback(ActionTypeInterface actionType, PromptCategory promptCategory);
}
