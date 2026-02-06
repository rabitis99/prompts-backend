package org.example.sharedprompts.domain.payment.infrastructure.monitoring.compensation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 보상 트랜잭션 작업 정보
 *
 * <p>후처리 실패 시 큐에 추가하여 나중에 재시도할 작업 정보를 담는 모델입니다.
 */
public record CompensationTask(
        /**
         * 작업 타입
         */
        CompensationTaskType taskType,
        
        /**
         * 결제 ID
         */
        Long paymentId,
        
        /**
         * 사용자 ID
         */
        Long userId,
        
        /**
         * 금액 (포인트 복구/적립 금액)
         */
        BigDecimal amount,
        
        /**
         * 원본 금액 (포인트 적립 시 포인트 기준 금액 계산에 필요)
         * null 가능 (포인트 복구 등에서는 불필요)
         */
        BigDecimal originalAmount,
        
        /**
         * 추가 파라미터 (JSON 형태로 저장 가능)
         */
        String metadata,
        
        /**
         * 실패 원인
         */
        String failureReason,
        
        /**
         * 생성 시각
         */
        LocalDateTime createdAt
) {
    public CompensationTask {
        if (taskType == null) {
            throw new IllegalArgumentException("taskType는 null일 수 없습니다");
        }
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId는 null일 수 없습니다");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId는 null일 수 없습니다");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount는 null일 수 없습니다");
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * 기본 보상 작업 생성 (originalAmount, metadata 없음)
     */
    public static CompensationTask of(CompensationTaskType taskType, Long paymentId,
                                       Long userId, BigDecimal amount, String failureReason) {
        return new CompensationTask(taskType, paymentId, userId, amount, null, null, failureReason, null);
    }

    /**
     * originalAmount가 필요한 보상 작업 생성
     */
    public static CompensationTask withOriginalAmount(CompensationTaskType taskType, Long paymentId,
                                                        Long userId, BigDecimal amount,
                                                        BigDecimal originalAmount, String failureReason) {
        return new CompensationTask(taskType, paymentId, userId, amount, originalAmount, null, failureReason, null);
    }
}

