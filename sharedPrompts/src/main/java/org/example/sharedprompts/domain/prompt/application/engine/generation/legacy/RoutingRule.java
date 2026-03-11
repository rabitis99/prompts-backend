package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.role.core.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.output.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import org.example.sharedprompts.domain.prompt.common.enums.role.metadata.DomainRoleType;

import java.util.Optional;

/** 단일 라우팅 룰: 조건 + override 값들 */
@Getter
@RequiredArgsConstructor
@ToString
public final class RoutingRule {

    private final String id;
    private final int priority;
    private final RoutingCondition condition;

    private final PromptObjective overrideObjective;
    private final OutputNeeds overrideOutputNeeds;
    private final ResponseShape overrideResponseShape;
    private final TaskDomain overrideDomain;
    private final EngineProfile overrideEngineProfile;
    private final CoreRoleType overrideCoreRole;
    private final DomainRoleType overrideDomainRole;

    public Optional<PromptObjective> getOverrideObjective() {
        return Optional.ofNullable(overrideObjective);
    }

    public Optional<OutputNeeds> getOverrideOutputNeeds() {
        return Optional.ofNullable(overrideOutputNeeds);
    }

    public Optional<ResponseShape> getOverrideResponseShape() {
        return Optional.ofNullable(overrideResponseShape);
    }

    public Optional<TaskDomain> getOverrideDomain() {
        return Optional.ofNullable(overrideDomain);
    }

    public Optional<EngineProfile> getOverrideEngineProfile() {
        return Optional.ofNullable(overrideEngineProfile);
    }

    public Optional<CoreRoleType> getOverrideCoreRole() {
        return Optional.ofNullable(overrideCoreRole);
    }

    public Optional<DomainRoleType> getOverrideDomainRole() {
        return Optional.ofNullable(overrideDomainRole);
    }

    public boolean matches(UnifiedGeneratePromptCommand command, IntentDefaults defaults) {
        return condition.matches(command, defaults);
    }
}
