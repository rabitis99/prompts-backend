package org.example.sharedprompts.domain.prompt.common.enums.action.catalog;

import java.util.List;

/**
 * ActionType enum 정의 집합 공급자.
 * "어떤 ActionType enum들이 존재하는가"만 제공한다.
 * Legacy compatibility 해석 순서는 {@link org.example.sharedprompts.domain.prompt.common.enums.action.resolver.OrderedActionTypeResolutionSource}로 별도 제공.
 */
public interface ActionTypeCatalog {

    /** 정의된 ActionType enum 클래스 목록. 순서는 registry 구성 등에 사용될 수 있으나, compatibility 해석 우선순위는 OrderedActionTypeResolutionSource로 결정한다. */
    List<Class<? extends Enum<?>>> getActionTypeEnumClasses();
}
