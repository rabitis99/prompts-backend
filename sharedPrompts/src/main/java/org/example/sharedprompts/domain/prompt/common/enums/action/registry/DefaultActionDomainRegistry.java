package org.example.sharedprompts.domain.prompt.common.enums.action.registry;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** stable key → TaskDomain 매핑. 정책은 enum이 아닌 이 레지스트리에서 관리. */
public final class DefaultActionDomainRegistry implements ActionDomainRegistry {

    /** stable key → TaskDomain 매핑 */
    private final Map<String, TaskDomain> keyToDomain;

    public DefaultActionDomainRegistry(Map<String, TaskDomain> keyToDomain) {
        this.keyToDomain = Map.copyOf(Objects.requireNonNull(keyToDomain, "keyToDomain"));
    }

    @Override
    public Optional<TaskDomain> getTaskDomain(ActionTypeInterface actionType) {
        if (actionType == null || actionType.key() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(keyToDomain.get(actionType.key()));
    }
}