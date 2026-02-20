package org.example.sharedprompts.module.domain.production.service.job.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.infra.messaging.RabbitMQConfig;
import org.example.sharedprompts.module.domain.production.service.job.queue.message.JobMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueuePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishJob(String jobId, Integer maxRetryCount) {
        try {
            JobMessage message = JobMessage.builder()
                    .jobId(jobId)
                    .createdAt(Instant.now())
                    .retryCount(0)
                    .maxRetryCount(maxRetryCount != null ? maxRetryCount : 3)
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.JOB_EXCHANGE,
                    RabbitMQConfig.JOB_ROUTING_KEY,
                    message
            );

            log.info("Job message published - jobId: {}, maxRetryCount: {}", jobId, message.getMaxRetryCount());

        } catch (Exception e) {
            log.error("Failed to publish job message - jobId: {}", jobId, e);
            throw new RuntimeException("Failed to publish job message: " + jobId, e);
        }
    }

    public void publishRetry(JobMessage message) {
        try {
            JobMessage retryMessage = message.withIncrementedRetry();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.JOB_EXCHANGE,
                    RabbitMQConfig.JOB_ROUTING_KEY,
                    retryMessage
            );

            log.info("Job retry message published - jobId: {}, retryCount: {}/{}",
                    retryMessage.getJobId(),
                    retryMessage.getRetryCount(),
                    retryMessage.getMaxRetryCount());

        } catch (Exception e) {
            log.error("Failed to publish job retry message - jobId: {}", message.getJobId(), e);
            throw new RuntimeException("Failed to publish job retry message: " + message.getJobId(), e);
        }
    }
}

