package org.example.sharedprompts.module.domain.production.service.job.process;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.job.JobStatus;
import org.example.sharedprompts.module.domain.production.service.job.JobStateService;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.JobExceptionHandler;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.RecoveryException;
import org.example.sharedprompts.module.domain.production.service.job.process.util.CommandDeserializer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobProcessor {

    private final JobStateService jobStateService;
    private final JobExceptionHandler exceptionHandler;
    private final CommandDeserializer commandDeserializer;
    private final JobProcessorDelegate jobProcessorDelegate;

    /**
     * Job 비동기 처리 위임
     * 
     * @Async 메서드는 JobProcessorDelegate로 위임하여
     * 자기 참조(self-proxy) 문제 없이 생성자 주입만 사용합니다.
     */
    public void processJobAsync(String jobId) {
        jobProcessorDelegate.processJobAsync(jobId);
    }

    /**
     * Job 복구 (FAILED 상태에서 재시도)
     * 
     * 설계 원칙:
     * - 중간 상태 복구 제거
     * - FAILED 상태에서만 retry()를 통해 처음부터 다시 처리
     */
    @Async
    public void recoverJob(String jobId) {
        JobEntity job = jobStateService.getJob(jobId);
        JobStatus status = job.getStatus();

        // FAILED 상태에서만 복구 가능
        if (status != JobStatus.FAILED) {
            log.warn("Cannot recover job - only FAILED status can be recovered. jobId: {}, status: {}", jobId, status);
            return;
        }

        log.info("Recovering failed job - jobId: {}, status: {}", jobId, status);

        try {
            // retry()를 통해 FAILED → PENDING으로 전이 후 처음부터 다시 처리
            jobStateService.retryJob(jobId);
            // 처음부터 다시 처리 - JobProcessorDelegate를 통해 @Async가 동작하도록 함
            // recoverJob은 이미 비동기 스레드에서 실행되지만, processJobAsync도 별도 스레드에서 실행되도록 함
            jobProcessorDelegate.processJobAsync(jobId);
        } catch (Exception e) {
            log.error("Failed to recover job - jobId: {}, status: {}", jobId, status, e);
            ProductionCommand command = commandDeserializer.deserialize(job);
            exceptionHandler.handleRecoveryException(jobId, command.getCommandType().name(), 
                    new RecoveryException("Recovery failed", e));
        }
    }
}

