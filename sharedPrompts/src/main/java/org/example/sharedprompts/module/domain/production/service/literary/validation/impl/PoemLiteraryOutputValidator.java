package org.example.sharedprompts.module.domain.production.service.literary.validation.impl;

import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.springframework.stereotype.Component;

@Component
public class PoemLiteraryOutputValidator extends AbstractLiteraryOutputValidator {

    /** Allows short forms (e.g. haiku, 2-line verse). Raise if product requires longer poems only. */
    private static final int MIN_CONTENT_LENGTH = 20;

    @Override
    public LiteraryType getLiteraryType() {
        return LiteraryType.POEM;
    }

    @Override
    protected int getMinContentLength() {
        return MIN_CONTENT_LENGTH;
    }

    @Override
    protected String getTypeName() {
        return "poem";
    }
}
