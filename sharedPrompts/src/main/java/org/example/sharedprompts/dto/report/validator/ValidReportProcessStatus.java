package org.example.sharedprompts.dto.report.request;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.example.sharedprompts.domain.report.enums.ReportStatus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidReportProcessStatus.Validator.class)
@Documented
public @interface ValidReportProcessStatus {
    String message() default "유효하지 않은 처리 상태입니다. PROCESSING, RESOLVED, REJECTED만 허용됩니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidReportProcessStatus, ReportStatus> {
        @Override
        public boolean isValid(ReportStatus status, ConstraintValidatorContext context) {
            if (status == null) {
                return true; // @NotNull이 처리
            }
            // PENDING은 처리 시작 상태로만 사용되므로, 처리 요청에서는 제외
            return status == ReportStatus.PROCESSING || 
                   status == ReportStatus.RESOLVED || 
                   status == ReportStatus.REJECTED;
        }
    }
}

