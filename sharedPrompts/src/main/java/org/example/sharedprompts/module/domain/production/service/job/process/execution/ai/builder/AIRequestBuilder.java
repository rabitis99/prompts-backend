package org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.builder;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.executor.image.ImageCommand;
import org.example.sharedprompts.module.domain.production.model.executor.literary.LiteraryCommand;
import org.example.sharedprompts.module.domain.production.model.executor.text.TextCommand;
import org.example.sharedprompts.module.domain.production.service.ai.ContentType;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AIRequestBuilder {

    public AIContentRequest build(JobEntity job, ProductionCommand command, String prompt, ContentType contentType) {
        // prompt는 필수 파라미터이므로 null 체크
        Objects.requireNonNull(prompt, "병합된 프롬프트는 null일 수 없습니다");

        AIContentRequest.AIContentRequestBuilder builder = AIContentRequest.builder()
                .contentType(contentType)
                .prompt(prompt)
                .userInput(job.getUserInput())
                .userId(job.getUserId())
                .jobId(job.getJobId());

        if (command instanceof TextCommand textCommand) {
            builder.contentTypeHint(textCommand.format());
        } else if (command instanceof LiteraryCommand literaryCommand) {
            builder.contentTypeHint("literary:" + Objects.requireNonNull(literaryCommand.literaryType(), "literaryType must not be null").name());
        } else if (command instanceof ImageCommand imageCommand) {
            builder
                    .width(imageCommand.width())
                    .height(imageCommand.height());
        }

        return builder.build();
    }
}

