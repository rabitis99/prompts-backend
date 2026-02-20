package org.example.sharedprompts.module.domain.production.service.job.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.infra.messaging.RabbitMQConfig;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.example.sharedprompts.module.domain.production.service.job.queue.message.JobMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobDlqConsumer {

    private final JobStateService jobStateService;

    @RabbitListener(queues = RabbitMQConfig.JOB_DLQ)
    public void consumeDlqMessage(@Payload JobMessage message) {
        String jobId = message.getJobId();

        try {
            log.warn("Processing DLQ message - jobId: {}, retryCount: {}/{}",
                    jobId, message.getRetryCount(), message.getMaxRetryCount());

            jobStateService.saveJobFailure(
                    jobId,
                    String.format("Job moved to DLQ after %d retries", message.getRetryCount())
            );

            log.warn("Job moved to DLQ and marked as FAILED - jobId: {}, retryCount: {}/{}",
                    jobId, message.getRetryCount(), message.getMaxRetryCount());

        } catch (Exception e) {
            log.error("Failed to process DLQ message - jobId: {}", jobId, e);
        }
    }
}

