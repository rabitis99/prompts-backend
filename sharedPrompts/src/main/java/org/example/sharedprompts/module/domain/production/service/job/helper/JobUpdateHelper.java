package org.example.sharedprompts.module.domain.production.service.job.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.repository.job.JobRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobUpdateHelper {

    private final JobRepository jobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJob(String jobId, Consumer<JobEntity> updater) {
        JobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        updater.accept(job);
        jobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T updateJobAndReturn(String jobId, Function<JobEntity, T> updater) {
        JobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        T result = updater.apply(job);
        jobRepository.save(job);
        return result;
    }
}

