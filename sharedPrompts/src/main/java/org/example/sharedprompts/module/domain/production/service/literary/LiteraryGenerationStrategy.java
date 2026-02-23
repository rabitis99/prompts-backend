package org.example.sharedprompts.module.domain.production.service.literary;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.executor.literary.LiteraryCommand;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;

public interface LiteraryGenerationStrategy {

    LiteraryType getLiteraryType();

    /**
     * Generate literary content. Default implementation: execute AI, then extract content.
     * Override for type-specific logic (e.g. novel chapter planning).
     */
    default LiteraryExecutionResult generate(JobEntity job, LiteraryCommand command, String composedPrompt,
                                             LiteraryAIExecutor aiExecutor, LiteraryResponseExtractor responseExtractor) {
        LiteraryExecutionResult execResult = aiExecutor.execute(job, command, composedPrompt);
        String content = responseExtractor.extractContent(execResult.content());
        return new LiteraryExecutionResult(content, execResult.modelName(), execResult.tokenUsage());
    }
}
