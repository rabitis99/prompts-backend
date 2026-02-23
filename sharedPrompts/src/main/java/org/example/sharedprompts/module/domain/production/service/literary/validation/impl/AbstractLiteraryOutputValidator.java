package org.example.sharedprompts.module.domain.production.service.literary.validation.impl;

import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.example.sharedprompts.module.domain.production.service.literary.validation.LiteraryOutputValidator;
import org.example.sharedprompts.module.domain.production.service.literary.validation.LiteraryValidationResult;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractLiteraryOutputValidator implements LiteraryOutputValidator {

    @Override
    public abstract LiteraryType getLiteraryType();

    protected abstract int getMinContentLength();

    protected abstract String getTypeName();

    @Override
    public LiteraryValidationResult validate(String rawContent) {
        List<String> errors = new ArrayList<>();
        if (rawContent == null || rawContent.isBlank()) {
            errors.add("Content is empty");
            return LiteraryValidationResult.failure(errors);
        }
        String trimmed = rawContent.trim();
        int minLength = getMinContentLength();
        if (trimmed.length() < minLength) {
            errors.add("Content too short (min " + minLength + " chars for " + getTypeName() + ")");
        }
        return errors.isEmpty() ? LiteraryValidationResult.ok() : LiteraryValidationResult.failure(errors);
    }
}
