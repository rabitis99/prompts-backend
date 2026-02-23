package org.example.sharedprompts.module.domain.production.service.literary.impl;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.executor.literary.LiteraryCommand;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryAIExecutor;
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryGenerationStrategy;
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryResponseExtractor;
import org.springframework.stereotype.Component;

@Component
public class ShortStoryGenerationStrategy implements LiteraryGenerationStrategy {

    private final LiteraryResponseExtractor responseExtractor;

    public ShortStoryGenerationStrategy(LiteraryResponseExtractor responseExtractor) {
        this.responseExtractor = responseExtractor;
    }

    @Override
    public LiteraryType getLiteraryType() {
        return LiteraryType.SHORT_STORY;
    }

    @Override
    public String generate(JobEntity job, LiteraryCommand command, String composedPrompt, LiteraryAIExecutor aiExecutor) {
        String raw = aiExecutor.execute(job, command, composedPrompt);
        return responseExtractor.extractContent(raw);
    }
}
