package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestType;

public class RequestTypeMustBeValidator implements ConstraintValidator<RequestTypeMustBe, RequestType> {

    private RequestType expected;

    @Override
    public void initialize(RequestTypeMustBe annotation) {
        this.expected = annotation.value();
    }

    @Override
    public boolean isValid(RequestType value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // @NotNull이 처리
        }
        return value == expected;
    }
}
