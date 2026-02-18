package org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.builder;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.executor.image.ImageCommand;
import org.example.sharedprompts.module.domain.production.model.executor.text.TextCommand;
import org.example.sharedprompts.module.domain.production.service.ai.ContentType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AIRequestBuilder {

    public AIContentRequest build(JobEntity job, ProductionCommand command, String prompt, ContentType contentType) {
        AIContentRequest.AIContentRequestBuilder builder = AIContentRequest.builder()
                .contentType(contentType)
                .promptId(job.getPromptId()) // 프롬프트 ID (참조용)
                .userId(job.getUserId()) // 사용자 ID
                .jobId(job.getJobId()) // Job ID (이미지 저장 시 사용)
                .prompt(prompt) // PromptTemplateService에서 병합된 프롬프트
                .userInput(job.getUserInput()); // 참조용 (이미 prompt에 병합됨)

        if (command instanceof TextCommand textCommand) {
            builder.contentTypeHint(textCommand.format());
        } else if (command instanceof ImageCommand imageCommand) {
            builder
                    .width(imageCommand.width())
                    .height(imageCommand.height());
        }

        return builder.build();
    }
}

