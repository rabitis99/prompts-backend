package org.example.sharedprompts.domain.payment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 실패한 결제 이벤트 저장 엔티티
 * 
 * 이벤트 리스너 처리 실패 시 이벤트를 보존하여 수동 재처리 가능하게 함
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "failed_payment_events", indexes = {
    @Index(name = "idx_failed_events_payment_id", columnList = "payment_id"),
    @Index(name = "idx_failed_events_processed_created", columnList = "processed, created_at")
})
public class FailedPaymentEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String eventData;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column
    private LocalDateTime processedAt;

    @Column
    @Builder.Default
    private Integer retryCount = 0;

    public void markProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }

    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
}

