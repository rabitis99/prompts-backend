package org.example.sharedprompts.module.domain.production.entity.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Job 큐 발행용 Outbox.
 * Job 생성과 동일 트랜잭션에서 저장되고, OutboxPublisher가 주기적으로 PENDING을 읽어 RabbitMQ로 발행한다.
 */
@Entity
@Table(name = "production_job_outbox", indexes = {
    @Index(name = "idx_outbox_status_created", columnList = "status, created_at")
})
@Getter
@Setter(AccessLevel.PACKAGE)
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class JobOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;

    @Column(name = "max_retry_count", nullable = false)
    private Integer maxRetryCount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";

    public void markSent() {
        setStatus(STATUS_SENT);
        setSentAt(LocalDateTime.now());
        setErrorMessage(null);
    }

    public void markFailed(String message) {
        setStatus(STATUS_FAILED);
        setErrorMessage(message != null && message.length() > 500 ? message.substring(0, 500) : message);
    }

    @PrePersist
    void onPersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
