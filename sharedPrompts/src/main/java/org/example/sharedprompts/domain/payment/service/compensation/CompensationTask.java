package org.example.sharedprompts.domain.payment.service.compensation;

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
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

