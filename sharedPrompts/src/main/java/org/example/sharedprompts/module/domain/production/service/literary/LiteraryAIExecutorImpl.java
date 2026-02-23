package org.example.sharedprompts.module.domain.production.service.literary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.executor.literary.LiteraryCommand;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.AIJobExecutor;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.AIServiceException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiteraryAIExecutorImpl implements LiteraryAIExecutor {

    private final AIJobExecutor aiJobExecutor;

    @Override
    public LiteraryExecutionResult execute(JobEntity job, LiteraryCommand command, String prompt) {
        var result = aiJobExecutor.execute(job, command, prompt);
        if (result == null || result.rawResponse() == null || result.rawResponse().isBlank()) {
            throw new AIServiceException("AI returned null or empty response");
        }
        return new LiteraryExecutionResult(
                result.rawResponse(),
                result.modelName(),
                result.tokenUsage() != null ? result.tokenUsage() : ""
        );
    }
}
