package org.example.sharedprompts.module.domain.production.service.literary.validation.impl;

import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.springframework.stereotype.Component;

@Component
public class NovelLiteraryOutputValidator extends AbstractLiteraryOutputValidator {

    /** First gate: reject obviously truncated output. Short story uses a lower threshold. */
    private static final int MIN_CONTENT_LENGTH = 2_000;

    @Override
    public LiteraryType getLiteraryType() {
        return LiteraryType.NOVEL;
    }

    @Override
    protected int getMinContentLength() {
        return MIN_CONTENT_LENGTH;
    }

    @Override
    protected String getTypeName() {
        return "novel";
    }
}
