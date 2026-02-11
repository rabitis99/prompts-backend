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
                .prompt(prompt)
                .userInput(job.getUserInput());

        if (command instanceof TextCommand textCommand) {
            builder.contentTypeHint(textCommand.format());
        } else if (command instanceof ImageCommand imageCommand) {
            builder
                    .prompt(imageCommand.prompt())
                    .width(imageCommand.width())
                    .height(imageCommand.height());
        }

        return builder.build();
    }
}

