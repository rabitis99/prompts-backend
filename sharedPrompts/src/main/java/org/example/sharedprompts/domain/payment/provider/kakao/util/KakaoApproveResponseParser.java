package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoApproveResponse;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * KakaoPay 결제 승인 응답 파서
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoApproveResponseParser {

    private static final String DEFAULT_SUCCESS_STATUS = "SUCCESS_PAYMENT";

    private final KakaoPayResponseParser responseParser;
    private final KakaoPayJsonConverter jsonConverter;

    public KakaoApproveResponse parse(String tid, String orderId, Map<String, Object> body) {
        validateErrorResponse(tid, orderId, body);
        String status = extractStatus(tid, orderId, body);

        return new KakaoApproveResponse(
                status,
                responseParser.parseApprovedAt(body),
                jsonConverter.convertToJson(body)
        );
    }

    private void validateErrorResponse(String tid, String orderId, Map<String, Object> body) {
        Object errorCodeObj = body.get("code");
        Object errorMsgObj = body.get("msg");
        Object errorObj = body.get("error");

        String errorCode = errorCodeObj != null ? errorCodeObj.toString() : null;
        String errorMsg = errorMsgObj != null ? errorMsgObj.toString() : null;
        String error = errorObj != null ? errorObj.toString() : null;

        if (errorCode != null || errorMsg != null || error != null) {
            String responseBodyJson = jsonConverter.convertToJson(body);
            log.error(
                    "KakaoPay approve 에러 응답: tid={}, orderId={}, code={}, msg={}, error={}",
                    SensitiveDataMasker.maskPaymentKey(tid), orderId, errorCode, 
                    SensitiveDataMasker.maskSensitiveData(errorMsg), 
                    SensitiveDataMasker.maskSensitiveData(error)
            );
            throw new ApiException(
                    ErrorCode.PAYMENT_PROVIDER_ERROR,
                    "KakaoPay approve 에러 응답: " + responseBodyJson
            );
        }
    }

    private String extractStatus(String tid, String orderId, Map<String, Object> body) {
        Object statusObj = body.get("status");
        String status = statusObj != null ? statusObj.toString() : null;
        if (status != null && !status.isEmpty()) {
            return status;
        }

        // status가 없으면 approved_at 기준으로 성공 처리
        Object approvedAt = body.get("approved_at");
        if (approvedAt != null) {
            log.warn(
                    "KakaoPay approve 응답에 status가 없어 approved_at 기준으로 성공 처리: tid={}, orderId={}",
                    SensitiveDataMasker.maskPaymentKey(tid), orderId
            );
            return DEFAULT_SUCCESS_STATUS;
        }

        // status와 approved_at 모두 없으면 오류
        String responseBodyJson = jsonConverter.convertToJson(body);
        log.error(
                "KakaoPay approve 응답에 status/approved_at 모두 없음: tid={}, orderId={}",
                SensitiveDataMasker.maskPaymentKey(tid), orderId
        );
        throw new ApiException(
                ErrorCode.PAYMENT_PROVIDER_RESPONSE_INVALID,
                "KakaoPay approve 응답 형식 오류: " + responseBodyJson
        );
    }
}

