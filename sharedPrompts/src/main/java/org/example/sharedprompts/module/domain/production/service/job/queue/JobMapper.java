package org.example.sharedprompts.module.domain.production.service.job.queue;

import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.job.Job;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class JobMapper {

    public Job toJob(JobEntity jobEntity) {
        return Job.builder()
                .jobId(jobEntity.getJobId())
                .promptId(jobEntity.getPromptId())
                .userId(jobEntity.getUserId())
                .command(null) // 필요 시 매핑
                .userInput(jobEntity.getUserInput())
                .status(jobEntity.getStatus())
                .errorMessage(jobEntity.getErrorMessage())
                .createdAt(jobEntity.getCreatedAt() != null ?
                        jobEntity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .startedAt(jobEntity.getStartedAt())
                .completedAt(jobEntity.getCompletedAt())
                .aiGeneratedContent(jobEntity.getAiGeneratedContent())
                .artifactId(jobEntity.getArtifactId())
                .build();
    }
}
