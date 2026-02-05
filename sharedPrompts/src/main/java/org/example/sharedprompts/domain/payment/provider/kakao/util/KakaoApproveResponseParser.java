package org.example.sharedprompts.domain.payment.provider.kakao.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoApproveResponse;
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
        String errorCode = (String) body.get("code");
        String errorMsg = (String) body.get("msg");
        String error = (String) body.get("error");

        if (errorCode != null || errorMsg != null || error != null) {
            String responseBodyJson = jsonConverter.convertToJson(body);
            log.error(
                    "KakaoPay approve 에러 응답: tid={}, orderId={}, code={}, msg={}, error={}",
                    tid, orderId, errorCode, errorMsg, error
            );
            throw new RuntimeException("KakaoPay approve 에러 응답: " + responseBodyJson);
        }
    }

    private String extractStatus(String tid, String orderId, Map<String, Object> body) {
        String status = (String) body.get("status");
        if (status != null && !status.isEmpty()) {
            return status;
        }

        // status가 없으면 approved_at 기준으로 성공 처리
        Object approvedAt = body.get("approved_at");
        if (approvedAt != null) {
            log.warn(
                    "KakaoPay approve 응답에 status가 없어 approved_at 기준으로 성공 처리: tid={}, orderId={}",
                    tid, orderId
            );
            return DEFAULT_SUCCESS_STATUS;
        }

        // status와 approved_at 모두 없으면 오류
        String responseBodyJson = jsonConverter.convertToJson(body);
        log.error(
                "KakaoPay approve 응답에 status/approved_at 모두 없음: tid={}, orderId={}",
                tid, orderId
        );
        throw new RuntimeException("KakaoPay approve 응답 형식 오류: " + responseBodyJson);
    }
}

