package org.example.sharedprompts.domain.payment.application.port.in.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 취소 명령
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelPaymentCommand {

    private Long paymentId;
    private Long userId;
    private String reason;

    /**
     * 최소 필수 정보로만 커맨드 생성
     */
    public static CancelPaymentCommand of(
            Long paymentId,
            Long userId
    ) {
        return CancelPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .reason("사용자 요청")
                .build();
    }
}
