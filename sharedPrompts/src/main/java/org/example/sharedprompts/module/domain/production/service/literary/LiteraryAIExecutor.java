package org.example.sharedprompts.module.domain.production.service.literary;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.executor.literary.LiteraryCommand;

public interface LiteraryAIExecutor {

    String execute(JobEntity job, LiteraryCommand command, String prompt);
}
