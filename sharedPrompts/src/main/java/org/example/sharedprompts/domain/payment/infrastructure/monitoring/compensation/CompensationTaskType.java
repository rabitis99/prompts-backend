package org.example.sharedprompts.domain.payment.infrastructure.monitoring.compensation;

/**
 * 보상 트랜잭션 작업 타입
 */
public enum CompensationTaskType {
    /**
     * 포인트 복구 (취소/환불 후처리 실패)
     */
    POINT_RECOVERY_CANCEL,
    
    /**
     * 포인트 복구 (환불 후처리 실패)
     */
    POINT_RECOVERY_REFUND,
    
    /**
     * 포인트 적립 (결제 성공 후처리 실패)
     */
    POINT_ACCRUAL,
    
    /**
     * 캐시백 적립 (결제 성공 후처리 실패)
     */
    CASHBACK_ACCRUAL
}

