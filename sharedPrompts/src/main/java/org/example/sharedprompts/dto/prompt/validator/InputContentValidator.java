package org.example.sharedprompts.dto.prompt.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class InputContentValidator implements ConstraintValidator<ValidInputContent, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;

        String text = value.replaceAll("\\s+", "");
        if (text.length() < 5) return false;
        if (text.matches("^[ㄱ-ㅎㅏ-ㅣ]+$")) return false;
        if (text.matches("^[a-zA-Z]+$")) return false;
        if (text.matches("^[!@#$%^&*()_+=\\-`~]+$")) return false;

        return true;
    }
}
