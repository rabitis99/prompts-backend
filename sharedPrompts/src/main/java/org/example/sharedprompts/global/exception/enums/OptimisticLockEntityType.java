package org.example.sharedprompts.global.exception.enums;

import lombok.Getter;
import org.example.sharedprompts.domain.report.Report;
import org.example.sharedprompts.global.exception.ErrorCode;

public enum OptimisticLockEntityType {

    REPORT(Report.class, ErrorCode.REPORT_ALREADY_PROCESSED);

    private final Class<?> entityClass;
    @Getter
    private final ErrorCode errorCode;

    OptimisticLockEntityType(Class<?> entityClass, ErrorCode errorCode) {
        this.entityClass = entityClass;
        this.errorCode = errorCode;
    }

    public static OptimisticLockEntityType fromEntity(Object entity) {
        if (entity == null) return null;

        for (OptimisticLockEntityType type : values()) {
            if (type.entityClass.isInstance(entity)) {
                return type;
            }
        }
        return null;
    }
}
