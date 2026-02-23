package org.example.sharedprompts.module.domain.production.service.literary.validation.impl;

import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.springframework.stereotype.Component;

@Component
public class ShortStoryLiteraryOutputValidator extends AbstractLiteraryOutputValidator {

    private static final int MIN_CONTENT_LENGTH = 100;

    @Override
    public LiteraryType getLiteraryType() {
        return LiteraryType.SHORT_STORY;
    }

    @Override
    protected int getMinContentLength() {
        return MIN_CONTENT_LENGTH;
    }

    @Override
    protected String getTypeName() {
        return "short story";
    }
}
