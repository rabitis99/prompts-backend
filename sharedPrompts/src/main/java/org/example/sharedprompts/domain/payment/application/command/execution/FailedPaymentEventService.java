package org.example.sharedprompts.domain.payment.application.command.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.entity.FailedPaymentEvent;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.failed.FailedPaymentEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 실패한 결제 이벤트 저장 서비스
 * 
 * 이벤트 처리 실패 시 이벤트를 DB에 저장하여 수동 재처리 가능하게 함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FailedPaymentEventService {

    private final FailedPaymentEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;
    private static final int MAX_STACK_TRACE_LENGTH = 4096;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedEvent(String eventType, Long paymentId, Object event, Exception exception, int retryCount) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            String errorMessage = exception.getMessage();
            String stackTrace = getStackTrace(exception);

            FailedPaymentEvent failedEvent = FailedPaymentEvent.builder()
                    .paymentId(paymentId)
                    .eventType(eventType)
                    .eventData(eventData)
                    .errorMessage(errorMessage)
                    .stackTrace(stackTrace)
                    .retryCount(retryCount)
                    .build();

            FailedPaymentEvent savedEvent = failedEventRepository.save(failedEvent);
            log.info("실패 이벤트 저장 완료: paymentId={}, eventType={}, failedEventId={}",
                    paymentId, eventType, savedEvent.getId());
        } catch (JsonProcessingException e) {
            log.error("실패 이벤트 직렬화 실패: paymentId={}, eventType={}", paymentId, eventType, e);
            saveFailedEventWithoutData(eventType, paymentId, exception, retryCount);
        } catch (Exception e) {
            log.error("실패 이벤트 저장 실패: paymentId={}, eventType={}", paymentId, eventType, e);
        }
    }

    private void saveFailedEventWithoutData(String eventType, Long paymentId, Exception exception, int retryCount) {
        try {
            FailedPaymentEvent failedEvent = FailedPaymentEvent.builder()
                    .paymentId(paymentId)
                    .eventType(eventType)
                    .eventData(null)
                    .errorMessage(exception.getMessage())
                    .stackTrace(getStackTrace(exception))
                    .retryCount(retryCount)
                    .build();
            failedEventRepository.save(failedEvent);
        } catch (Exception e) {
            log.error("실패 이벤트 저장 최종 실패: paymentId={}, eventType={}", paymentId, eventType, e);
        }
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
        }
        String trace = sw.toString();
        return trace.length() > MAX_STACK_TRACE_LENGTH
                ? trace.substring(0, MAX_STACK_TRACE_LENGTH)
                : trace;
    }
}

