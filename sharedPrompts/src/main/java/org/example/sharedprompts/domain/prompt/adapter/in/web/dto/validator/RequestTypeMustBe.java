package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequestTypeMustBeValidator.class)
@Documented
public @interface RequestTypeMustBe {
    RequestType value();
    String message() default "request_type이 올바르지 않습니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
