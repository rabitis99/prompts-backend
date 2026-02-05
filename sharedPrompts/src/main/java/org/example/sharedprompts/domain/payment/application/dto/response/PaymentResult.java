package org.example.sharedprompts.domain.payment.application.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 Provider 응답을 공통 도메인 모델로 변환한 결과
 * 외부 API 응답과 도메인 로직을 분리하기 위한 모델
 */
@Getter
@Builder
public class PaymentResult {
    
    /**
     * 외부 결제사에서 발급한 결제 ID
     */
    private final String externalPaymentId;
    
    /**
     * 결제 상태
     */
    private final PaymentStatus status;
    
    /**
     * 결제 금액
     */
    private final BigDecimal amount;
    
    /**
     * 통화 코드
     */
    private final String currency;
    
    /**
     * 주문 ID
     */
    private final String orderId;
    
    /**
     * 승인 시간
     */
    private final LocalDateTime approvedAt;
    
    /**
     * 실패 사유 (실패 시)
     */
    private final String failureReason;
    
    /**
     * 추가 메타데이터 (JSON 형태)
     */
    private final String metadata;
    
    /**
     * 성공 여부
     */
    public boolean isSuccess() {
        return status == PaymentStatus.SUCCESS;
    }
    
    /**
     * 실패 여부
     */
    public boolean isFailure() {
        return status == PaymentStatus.FAILED;
    }
}

