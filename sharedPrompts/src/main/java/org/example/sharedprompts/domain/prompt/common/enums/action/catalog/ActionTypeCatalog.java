package org.example.sharedprompts.domain.prompt.common.enums.action.catalog;

import java.util.List;

/** ActionType enum 단일 등록원. 스캔/자동 발견 없음. */
public interface ActionTypeCatalog {

    /** catalog 순서의 ActionType enum 클래스 목록. */
    List<Class<? extends Enum<?>>> getActionTypeEnumClasses();
}
