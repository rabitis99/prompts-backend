package org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.dto.KakaoCancelResponse;
import org.example.sharedprompts.domain.payment.infrastructure.external.provider.kakao.dto.KakaoRefundResponse;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
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
        validateErrorResponse(tid, body);
        log.info("KakaoPay cancel success: tid={}", SensitiveDataMasker.maskPaymentKey(tid));
        return new KakaoCancelResponse(
                responseParser.parseCanceledAt(body),
                jsonConverter.convertToJson(body)
        );
    }

    public KakaoRefundResponse parseRefund(String tid, long requestedAmount, Map<String, Object> body) {
        validateErrorResponse(tid, body);
        long refundedAmount = extractRefundedAmount(Objects.requireNonNull(body), requestedAmount);
        log.info("KakaoPay refund success: tid={}, amount={}", 
                SensitiveDataMasker.maskPaymentKey(tid), refundedAmount);
        return new KakaoRefundResponse(
                refundedAmount,
                responseParser.parseCanceledAt(body),
                jsonConverter.convertToJson(body)
        );
    }

    private void validateErrorResponse(String tid, Map<String, Object> body) {
        Object errorCodeObj = body.get("code");
        Object errorMsgObj = body.get("msg");
        Object errorObj = body.get("error");

        String errorCode = errorCodeObj != null ? errorCodeObj.toString() : null;
        String errorMsg = errorMsgObj != null ? errorMsgObj.toString() : null;
        String error = errorObj != null ? errorObj.toString() : null;

        if (errorCode != null || errorMsg != null || error != null) {
            String responseBodyJson = jsonConverter.convertToJson(body);
            log.error(
                    "KakaoPay cancel/refund 에러 응답: tid={}, code={}, msg={}, error={}",
                    SensitiveDataMasker.maskPaymentKey(tid), errorCode, 
                    SensitiveDataMasker.maskSensitiveData(errorMsg), 
                    SensitiveDataMasker.maskSensitiveData(error)
            );
            throw new ApiException(
                    ErrorCode.PAYMENT_PROVIDER_ERROR,
                    "KakaoPay cancel/refund 에러 응답: " + responseBodyJson
            );
        }
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

