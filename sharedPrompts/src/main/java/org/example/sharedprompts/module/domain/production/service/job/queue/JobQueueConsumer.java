package org.example.sharedprompts.module.domain.production.service.job.queue;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.infra.messaging.RabbitMQConfig;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.tenant.TenantContext;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.example.sharedprompts.module.domain.production.service.job.process.JobProcessorDelegate;
import org.example.sharedprompts.module.domain.production.service.job.queue.message.JobMessage;
import org.example.sharedprompts.module.domain.production.service.job.queue.retry.ReactiveJobRetryService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobQueueConsumer {

    private final JobProcessorDelegate jobProcessorDelegate;
    private final JobStateService jobStateService;
    private final ReactiveJobRetryService reactiveJobRetryService;

    @RabbitListener(
            queues = RabbitMQConfig.JOB_QUEUE,
            containerFactory = "jobWorkerContainerFactory"
    )
    public void consumeJobMessage(
            @Payload JobMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) {
        String jobId = message.getJobId();
        String tenantId = null;

        try {
            log.info("Consuming job message - jobId: {}, retryCount: {}/{}",
                    jobId, message.getRetryCount(), message.getMaxRetryCount());

            JobEntity job = jobStateService.getJob(jobId);

            if (job.isFinalState()) {
                log.info("Job already finished - jobId: {}, status: {}", jobId, job.getStatus());
                acknowledgeMessage(channel, deliveryTag);
                return;
            }

            tenantId = job.getTenantId();
            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.setCurrentTenantId(tenantId);
                log.debug("Tenant context set - jobId: {}, tenantId: {}", jobId, tenantId);
            }

            jobProcessorDelegate.processJob(jobId);

            acknowledgeMessage(channel, deliveryTag);
            log.info("Job message processed successfully - jobId: {}", jobId);

        } catch (Exception e) {
            handleJobProcessingException(jobId, message, e, channel, deliveryTag);
        } finally {
            TenantContext.clear();
        }
    }

    private void handleJobProcessingException(
            String jobId,
            JobMessage message,
            Exception e,
            Channel channel,
            long deliveryTag
    ) {
        log.error("Job processing failed - jobId: {}, retryCount: {}/{}, error: {}",
                jobId, message.getRetryCount(), message.getMaxRetryCount(), e.getMessage(), e);

        try {
            if (message.isMaxRetryExceeded()) {
                log.warn("Max retry count exceeded - jobId: {}, retryCount: {}/{}. Moving to DLQ.",
                        jobId, message.getRetryCount(), message.getMaxRetryCount());

                updateJobToFailed(jobId, "Max retry count exceeded: " + e.getMessage());
                nackMessage(channel, deliveryTag, false);
            } else {
                log.info("Scheduling retry - jobId: {}, retryCount: {}/{}",
                        jobId, message.getRetryCount(), message.getMaxRetryCount());

                reactiveJobRetryService.scheduleRetry(message)
                        .doOnSuccess(unused -> log.debug("Retry scheduled successfully - jobId: {}", jobId))
                        .doOnError(error -> {
                            log.error("Failed to schedule retry - jobId: {}", jobId, error);
                            updateJobToFailed(jobId, "Retry scheduling failed: " + error.getMessage());
                        })
                        .subscribe();
                
                nackMessage(channel, deliveryTag, false);
            }
        } catch (Exception handlerException) {
            log.error("Exception handler failed - jobId: {}", jobId, handlerException);
            updateJobToFailed(jobId, "Exception handler failed: " + handlerException.getMessage());
            nackMessage(channel, deliveryTag, false);
        }
    }

    private void updateJobToFailed(String jobId, String errorMessage) {
        try {
            jobStateService.saveJobFailure(jobId, errorMessage);
        } catch (Exception e) {
            log.error("Failed to update job status to FAILED - jobId: {}", jobId, e);
        }
    }

    private void acknowledgeMessage(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("Failed to acknowledge message - deliveryTag: {}", deliveryTag, e);
        }
    }

    private void nackMessage(Channel channel, long deliveryTag, boolean requeue) {
        try {
            channel.basicNack(deliveryTag, false, requeue);
        } catch (IOException e) {
            log.error("Failed to nack message - deliveryTag: {}, requeue: {}", deliveryTag, requeue, e);
        }
    }
}

