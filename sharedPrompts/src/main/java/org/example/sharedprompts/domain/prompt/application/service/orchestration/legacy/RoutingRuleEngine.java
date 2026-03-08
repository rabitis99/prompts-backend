package org.example.sharedprompts.domain.prompt.application.service.orchestration.legacy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.role.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.DomainRoleType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RuleTable 기반 라우팅 예외/override 엔진.
 *
 * <p>현재 구현은 인메모리 룰 리스트를 사용하며, 추후 DB/설정 기반으로 확장 가능하다.</p>
 */
@Slf4j
@Component
public class RoutingRuleEngine {

    private final List<RoutingRule> rules = new CopyOnWriteArrayList<>();

    /**
     * 기본 생성자 – 운영 환경에서는 별도 룰이 없다.
     * <p>룰이 비어있으면 Override 도 비어있다.</p>
     */
    public RoutingRuleEngine() {
    }

    /**
     * 테스트/설정 주입용 생성자.
     * <p>초기 룰도 {@link #registerRule(RoutingRule)} 검증 경로를 거쳐 무효 룰으로 인한 NPE를 방지한다.</p>
     */
    public RoutingRuleEngine(List<RoutingRule> initialRules) {
        if (initialRules != null) {
            initialRules.forEach(this::registerRule);
        }
    }

    /**
     * 추후 설정/관리자를 통해 룰을 등록하기 위한 진입점.
     * <p>현재 코드는 운영 경로에서는 사용하지 않고 테스트에서만 사용한다.</p>
     *
     * @param rule 등록할 룰 (null 불가, id·condition 필수)
     * @throws NullPointerException     rule 또는 rule.condition이 null인 경우
     * @throws IllegalArgumentException rule.id가 null이거나 blank인 경우
     */
    public void registerRule(RoutingRule rule) {
        Objects.requireNonNull(rule, "rule must not be null");
        if (rule.getId() == null || rule.getId().isBlank()) {
            throw new IllegalArgumentException("rule.id must not be blank");
        }
        Objects.requireNonNull(rule.getCondition(), "rule.condition must not be null");
        boolean duplicateExists = this.rules.stream()
                .anyMatch(r -> r.getId().equals(rule.getId()));
        if (duplicateExists) {
            throw new IllegalArgumentException("duplicate rule.id: " + rule.getId());
        }
        this.rules.add(rule);
    }

    public RoutingOverrides apply(UnifiedGeneratePromptCommand command, IntentDefaults defaults) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(defaults, "defaults must not be null");
        if (rules.isEmpty()) {
            return RoutingOverrides.empty();
        }

        List<MatchedRule> matched = new ArrayList<>();
        for (int index = 0; index < rules.size(); index++) {
            RoutingRule rule = rules.get(index);
            if (rule.matches(command, defaults)) {
                matched.add(new MatchedRule(rule, index));
            }
        }

        if (matched.isEmpty()) {
            return RoutingOverrides.empty();
        }

        // 1) priority 높은 것 우선
        // 2) 동일 priority 시 조건 필드 수 많은 rule 우선
        // 3) 최종 tie 는 등록 순서 (index) 기준
        matched.sort(Comparator
                .comparingInt((MatchedRule m) -> m.rule.getPriority()).reversed()
                .thenComparing(
                        Comparator.comparingInt((MatchedRule m) -> m.rule.getCondition().specificity()).reversed()
                )
                .thenComparingInt(m -> m.index));

        RoutingRule winner = matched.get(0).rule;

        List<String> appliedRuleIds = matched.stream()
                .map(m -> m.rule.getId())
                .toList();

        log.debug("[UnifiedRouting][RuleEngine] matchedRules={}, winner={}",
                appliedRuleIds, winner.getId());

        return new RoutingOverrides(
                winner.getOverrideObjective().orElse(null),
                winner.getOverrideOutputNeeds().orElse(null),
                winner.getOverrideResponseShape().orElse(null),
                winner.getOverrideDomain().orElse(null),
                winner.getOverrideEngineProfile().orElse(null),
                winner.getOverrideCoreRole().orElse(null),
                winner.getOverrideDomainRole().orElse(null),
                appliedRuleIds
        );
    }

    @Getter
    @RequiredArgsConstructor
    @ToString
    public static final class RoutingRule {
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

    @Getter
    @RequiredArgsConstructor
    @ToString
    public static final class RoutingCondition {
        private final String intentName;
        private final String categoryKey;
        private final Boolean hasJsonSchema;
        private final TaskDomain domainAffinity;

        /**
         * 조건에 사용되는 필드 수 (null 이 아닌 필드 개수).
         */
        public int specificity() {
            int count = 0;
            if (intentName != null) count++;
            if (categoryKey != null) count++;
            if (hasJsonSchema != null) count++;
            if (domainAffinity != null) count++;
            return count;
        }

        public boolean matches(UnifiedGeneratePromptCommand command, IntentDefaults defaults) {
            if (intentName != null && (command.intent() == null
                    || !intentName.equals(command.intent().name()))) {
                return false;
            }
            if (categoryKey != null && (command.category() == null
                    || !categoryKey.equals(command.category().key()))) {
                return false;
            }
            if (hasJsonSchema != null) {
                boolean commandHasSchema = command.jsonSchema() != null && !command.jsonSchema().isBlank();
                if (!Objects.equals(hasJsonSchema, commandHasSchema)) {
                    return false;
                }
            }
            if (domainAffinity != null && !Objects.equals(domainAffinity, defaults.domainAffinity())) {
                return false;
            }
            return true;
        }

        public static RoutingCondition empty() {
            return new RoutingCondition(null, null, null, null);
        }
    }

    private record MatchedRule(RoutingRule rule, int index) {
    }

    public record RoutingOverrides(
            PromptObjective objectiveOverride,
            OutputNeeds outputNeedsOverride,
            ResponseShape responseShapeOverride,
            TaskDomain domainOverride,
            EngineProfile engineProfileOverride,
            CoreRoleType coreRoleOverride,
            DomainRoleType domainRoleOverride,
            List<String> appliedRuleIds
    ) {

        public RoutingOverrides {
            appliedRuleIds = appliedRuleIds != null ? List.copyOf(appliedRuleIds) : Collections.emptyList();
        }

        public static RoutingOverrides empty() {
            return new RoutingOverrides(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Collections.emptyList()
            );
        }
    }
}

