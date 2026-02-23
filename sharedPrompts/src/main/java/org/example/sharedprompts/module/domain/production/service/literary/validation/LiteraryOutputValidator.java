package org.example.sharedprompts.module.domain.production.service.literary.validation;

import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;

public interface LiteraryOutputValidator {

    LiteraryType getLiteraryType();

    LiteraryValidationResult validate(String rawContent);
}
