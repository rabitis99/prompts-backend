package org.example.sharedprompts.module.domain.production.service.literary.impl;

import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryGenerationStrategy;
import org.springframework.stereotype.Component;

@Component
public class ScriptGenerationStrategy implements LiteraryGenerationStrategy {

    @Override
    public LiteraryType getLiteraryType() {
        return LiteraryType.SCRIPT;
    }
}
