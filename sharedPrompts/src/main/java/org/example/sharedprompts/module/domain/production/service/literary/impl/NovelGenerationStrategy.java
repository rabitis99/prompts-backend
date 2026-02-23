package org.example.sharedprompts.module.domain.production.service.literary.impl;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.executor.literary.LiteraryCommand;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryAIExecutor;
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryGenerationStrategy;
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryResponseExtractor;
import org.example.sharedprompts.module.domain.production.service.literary.novel.TokenEstimator;
import org.springframework.stereotype.Component;

@Component
public class NovelGenerationStrategy implements LiteraryGenerationStrategy {

    private final LiteraryResponseExtractor responseExtractor;
    private final TokenEstimator tokenEstimator;

    public NovelGenerationStrategy(LiteraryResponseExtractor responseExtractor, TokenEstimator tokenEstimator) {
        this.responseExtractor = responseExtractor;
        this.tokenEstimator = tokenEstimator;
    }

    @Override
    public LiteraryType getLiteraryType() {
        return LiteraryType.NOVEL;
    }

    @Override
    public String generate(JobEntity job, LiteraryCommand command, String composedPrompt, LiteraryAIExecutor aiExecutor) {
        String raw = aiExecutor.execute(job, command, composedPrompt);
        String content = responseExtractor.extractContent(raw);
        if (tokenEstimator.estimateTokens(content) > tokenEstimator.getMaxTokensPerRequest()) {
        }
        return content;
    }
}
