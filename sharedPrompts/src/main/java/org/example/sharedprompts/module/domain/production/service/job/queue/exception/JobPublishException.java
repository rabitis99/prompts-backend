package org.example.sharedprompts.module.domain.production.service.job.queue.exception;

import org.example.sharedprompts.module.domain.production.service.job.process.exception.JobProcessingException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.JOB_QUEUE_PUBLISH_ERROR;

/**
 * Job 큐 발행 실패 예외
 * 
 * RabbitMQ에 Job 메시지를 발행하는 과정에서 발생하는 예외를 나타냅니다.
 * 호출자가 발행 실패를 구분하여 처리할 수 있도록 도메인 예외로 정의합니다.
 */
public class JobPublishException extends JobProcessingException {
    
    public JobPublishException(String message) {
        super(JOB_QUEUE_PUBLISH_ERROR, message);
    }

    public JobPublishException(String message, Throwable cause) {
        super(JOB_QUEUE_PUBLISH_ERROR, message, cause);
    }

    public JobPublishException(ModuleErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public JobPublishException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}


