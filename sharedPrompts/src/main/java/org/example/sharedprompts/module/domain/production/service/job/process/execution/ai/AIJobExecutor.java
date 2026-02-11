package org.example.sharedprompts.module.domain.production.service.job.process.execution.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentResult;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.service.ai.AIService;
import org.example.sharedprompts.module.domain.production.service.ai.AIServiceRegistry;
import org.example.sharedprompts.module.domain.production.service.ai.ContentType;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.AIServiceException;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.builder.AIRequestBuilder;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.circuitbreaker.AICircuitBreakerWrapper;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.retry.AIRetryExecutor;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.token.TokenExtractor;
import org.example.sharedprompts.module.domain.production.service.job.process.util.ContentTypeDeterminer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIJobExecutor {

    private final AIServiceRegistry aiServiceRegistry;
    private final AICircuitBreakerWrapper circuitBreakerWrapper;
    private final AIRetryExecutor retryExecutor;
    private final AIRequestBuilder requestBuilder;
    private final TokenExtractor tokenExtractor;
    private final ContentTypeDeterminer contentTypeDeterminer;

    public AIExecutionResult execute(JobEntity job, ProductionCommand command, String prompt) {
        log.info("Executing AI call - jobId: {}, commandType: {}", job.getJobId(), command.getCommandType());

        ContentType contentType = contentTypeDeterminer.determine(command);
        AIService aiService = aiServiceRegistry.getService(contentType);

        AIContentRequest request = requestBuilder.build(job, command, prompt, contentType);
        AIContentResult result = circuitBreakerWrapper.execute("aiProduction", 
                () -> retryExecutor.executeWithRetry(aiService, request));

        if (!result.isSuccess()) {
            throw new AIServiceException("AI call failed: " + result.getErrorMessage());
        }

        String rawResponse = result.getContent();
        String modelName = aiService.getModelName();
        String tokenUsage = tokenExtractor.extractTokenUsage(result);

        return new AIExecutionResult(rawResponse, modelName, tokenUsage);
    }

    public record AIExecutionResult(
            String rawResponse,
            String modelName,
            String tokenUsage
    ) {}
}
