package org.example.sharedprompts.module.domain.production.service.literary.validation.impl;

import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.example.sharedprompts.module.domain.production.service.literary.validation.LiteraryOutputValidator;
import org.example.sharedprompts.module.domain.production.service.literary.validation.LiteraryValidationResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultLiteraryOutputValidator implements LiteraryOutputValidator {

    private static final int MIN_CONTENT_LENGTH = 50;

    @Override
    public LiteraryType getLiteraryType() {
        return LiteraryType.POEM;
    }

    @Override
    public LiteraryValidationResult validate(String rawContent) {
        List<String> errors = new ArrayList<>();
        if (rawContent == null || rawContent.isBlank()) {
            errors.add("Content is empty");
            return LiteraryValidationResult.failure(errors);
        }
        String t = rawContent.trim();
        if (t.length() < MIN_CONTENT_LENGTH) {
            errors.add("Content too short (min " + MIN_CONTENT_LENGTH + " chars)");
        }
        return errors.isEmpty() ? LiteraryValidationResult.ok() : LiteraryValidationResult.failure(errors);
    }
}
