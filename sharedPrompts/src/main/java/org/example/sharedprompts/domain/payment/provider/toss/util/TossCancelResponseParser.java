package org.example.sharedprompts.domain.payment.provider.toss.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.provider.toss.dto.TossCancelResponse;
import org.example.sharedprompts.domain.payment.provider.toss.dto.TossRefundResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * TossPay 결제 취소/환불 응답 파서
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossCancelResponseParser {

    private final TossPayResponseParser responseParser;
    private final TossPayJsonConverter jsonConverter;

    public TossCancelResponse parseCancel(String paymentKey, Map<String, Object> body) {
        log.info("TossPay 결제 취소 성공: paymentKey={}", paymentKey);
        return new TossCancelResponse(
                responseParser.parseCanceledAt(body),
                jsonConverter.convertToJson(body)
        );
    }

    public TossRefundResponse parseRefund(String paymentKey, long requestedAmount, Map<String, Object> body) {
        long actualRefundedAmount = responseParser.parseCanceledAmount(body, requestedAmount);
        log.info(
                "TossPay 결제 환불 성공: paymentKey={}, requestedAmount={}, actualRefundedAmount={}",
                paymentKey, requestedAmount, actualRefundedAmount
        );
        return new TossRefundResponse(
                actualRefundedAmount,
                responseParser.parseCanceledAt(body),
                jsonConverter.convertToJson(body)
        );
    }
}

