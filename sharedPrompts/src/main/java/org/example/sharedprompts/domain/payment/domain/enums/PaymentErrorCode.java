package org.example.sharedprompts.domain.payment.domain.enums;

/**
 * 결제 도메인 내부 에러 코드 (이벤트/로깅용).
 * API 응답용 ErrorCode와 구분하여, 결제 실패 사유 등을 타입 안전하게 관리합니다.
 */
public enum PaymentErrorCode {
    /** 결제사 게이트웨이 승인 요청 실패 */
    GATEWAY_CONFIRM_FAILED,
}
