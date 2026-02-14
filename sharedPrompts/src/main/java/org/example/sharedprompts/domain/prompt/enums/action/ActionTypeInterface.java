package org.example.sharedprompts.domain.prompt.enums.action;

import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;

import java.util.Optional;

/**
 * ActionType 공통 인터페이스
 * 모든 ActionType enum이 구현해야 하는 공통 메서드 정의
 */
public interface ActionTypeInterface {
    String getDisplayNameKo();
    String getDisplayNameEn();
    String getDisplayNameJa();

    /**
     * 이 ActionType이 속하는 TaskDomain을 반환한다.
     * <ul>
     *   <li>{@code Optional.of(X)} — 명시적으로 지정된 도메인</li>
     *   <li>{@code Optional.empty()} — 매핑 누락 (빌드 타임 테스트가 강제)</li>
     * </ul>
     */
    default Optional<TaskDomain> getTaskDomain() {
        return Optional.empty();
    }

    /**
     * 언어 타입에 따라 적절한 표시 이름을 반환하는 디폴트 메서드
     */
    default String getDisplayNameByLang(LanguageType lang) {
        if (lang == null) {
            return getDisplayNameKo();
        }
        return switch (lang) {
            case KOREAN -> getDisplayNameKo();
            case ENGLISH -> getDisplayNameEn();
            case JAPANESE -> getDisplayNameJa();
        };
    }
}
