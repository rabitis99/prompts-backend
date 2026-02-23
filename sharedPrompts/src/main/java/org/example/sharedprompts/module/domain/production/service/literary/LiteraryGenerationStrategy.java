package org.example.sharedprompts.module.domain.production.service.literary;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.executor.literary.LiteraryCommand;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;

public interface LiteraryGenerationStrategy {

    LiteraryType getLiteraryType();

    String generate(JobEntity job, LiteraryCommand command, String composedPrompt, LiteraryAIExecutor aiExecutor);
}
