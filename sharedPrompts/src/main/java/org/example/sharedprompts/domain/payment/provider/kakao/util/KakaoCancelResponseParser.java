package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoCancelResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoRefundResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * KakaoPay 결제 취소/환불 응답 파서
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoCancelResponseParser {

    private final KakaoPayResponseParser responseParser;
    private final KakaoPayJsonConverter jsonConverter;

    public KakaoCancelResponse parseCancel(String tid, Map<String, Object> body) {
        log.info("KakaoPay cancel success: tid={}", tid);
        return new KakaoCancelResponse(
                responseParser.parseCanceledAt(body),
                jsonConverter.convertToJson(body)
        );
    }

    public KakaoRefundResponse parseRefund(String tid, long requestedAmount, Map<String, Object> body) {
        long refundedAmount = extractRefundedAmount(Objects.requireNonNull(body), requestedAmount);
        log.info("KakaoPay refund success: tid={}, amount={}", tid, refundedAmount);
        return new KakaoRefundResponse(
                refundedAmount,
                responseParser.parseCanceledAt(body),
                jsonConverter.convertToJson(body)
        );
    }

    @SuppressWarnings("unchecked")
    private long extractRefundedAmount(Map<String, Object> body, long fallbackAmount) {
        Map<String, Object> canceledAmount =
                (Map<String, Object>) body.get("canceled_amount");

        if (canceledAmount != null && canceledAmount.get("total") != null) {
            return Long.parseLong(canceledAmount.get("total").toString());
        }
        return fallbackAmount;
    }
}

