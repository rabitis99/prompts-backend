package org.example.sharedprompts.domain.prompt.domain.objective;

import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * ObjectiveProfile을 EnumMap으로 관리하는 순수 Java 레지스트리.
 *
 * <p>Spring 의존 없음 — {@code PromptDomainConfig}의 @Bean 메서드에서 인스턴스를 생성한다.
 *
 * <p><b>Fail-fast 보장:</b> 생성 시점에 {@link PromptObjective}의 모든 값에 대응하는
 * 프로파일이 없으면 {@link IllegalStateException}을 발생시킨다.
 * → 새 Objective 추가 후 레지스트리 등록을 빠뜨리면 앱 기동 직후 즉시 감지된다.
 */
public final class DefaultObjectiveRegistry implements ObjectiveRegistry {

    private final Map<PromptObjective, ObjectiveProfile> index;

    public DefaultObjectiveRegistry(List<ObjectiveProfile> profiles) {
        EnumMap<PromptObjective, ObjectiveProfile> map = new EnumMap<>(PromptObjective.class);
        for (ObjectiveProfile p : profiles) {
            map.put(p.objective(), p);
        }
        this.index = Map.copyOf(map);
        validate();
    }

    @Override
    public ObjectiveProfile get(PromptObjective objective) {
        ObjectiveProfile profile = index.get(objective);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "ObjectiveProfile 미등록: " + objective + ". PromptDomainConfig에 추가하세요.");
        }
        return profile;
    }

    /** 앱 기동 시 모든 PromptObjective 값에 프로파일이 존재하는지 검증 */
    private void validate() {
        for (PromptObjective o : PromptObjective.values()) {
            if (!index.containsKey(o)) {
                throw new IllegalStateException(
                        "ObjectiveProfile 미등록: " + o + ". PromptDomainConfig에 추가하세요.");
            }
        }
    }
}
