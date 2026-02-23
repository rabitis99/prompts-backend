package org.example.sharedprompts.module.domain.production.service.literary.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.executor.literary.LiteraryCommand;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryAIExecutor;
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryExecutionResult;
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryGenerationStrategy;
import org.example.sharedprompts.module.domain.production.service.literary.LiteraryResponseExtractor;
import org.example.sharedprompts.module.domain.production.service.literary.novel.TokenEstimator;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NovelGenerationStrategy implements LiteraryGenerationStrategy {

    private final TokenEstimator tokenEstimator;

    public NovelGenerationStrategy(TokenEstimator tokenEstimator) {
        this.tokenEstimator = tokenEstimator;
    }

    @Override
    public LiteraryType getLiteraryType() {
        return LiteraryType.NOVEL;
    }

    @Override
    public LiteraryExecutionResult generate(JobEntity job, LiteraryCommand command, String composedPrompt,
                                            LiteraryAIExecutor aiExecutor, LiteraryResponseExtractor responseExtractor) {
        LiteraryExecutionResult execResult = aiExecutor.execute(job, command, composedPrompt);
        String content = responseExtractor.extractContent(execResult.content());
        int estimatedTokens = tokenEstimator.estimateTokens(content);
        if (estimatedTokens > tokenEstimator.getMaxTokensPerRequest()) {
            log.warn("Novel content exceeds token limit (estimated: {}, max: {}) - consider chunking in a future iteration",
                    estimatedTokens, tokenEstimator.getMaxTokensPerRequest());
        }
        return new LiteraryExecutionResult(content, execResult.modelName(), execResult.tokenUsage());
    }
}
