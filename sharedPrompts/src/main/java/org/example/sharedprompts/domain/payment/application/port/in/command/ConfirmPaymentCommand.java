package org.example.sharedprompts.domain.payment.application.port.in.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 승인 확인 명령
 * 결제 제공자(PG)로부터 승인 완료 후 확인할 때 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentCommand {

    private Long paymentId;
    private Long userId;
    private String providerToken;
    private String rawPayload;

    /**
     * 최소 필수 정보로만 커맨드 생성
     */
    public static ConfirmPaymentCommand of(
            Long paymentId,
            Long userId,
            String providerToken
    ) {
        return ConfirmPaymentCommand.builder()
                .paymentId(paymentId)
                .userId(userId)
                .providerToken(providerToken)
                .build();
    }
}
