package org.example.sharedprompts.domain.payment.provider.kakao.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.properties.KakaoPayProperties;
import org.example.sharedprompts.domain.payment.provider.kakao.dto.KakaoStatusResponse;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoPayHeadersProvider;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoStatusErrorHandler;
import org.example.sharedprompts.domain.payment.provider.kakao.util.KakaoStatusResponseParser;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * KakaoPay 결제 상태 조회 API Client
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class KakaoStatusApiClient {

    private static final String KAKAO_PAY_API_URL = "https://open-api.kakaopay.com/online/v1/payment";
    private static final String STATUS_ENDPOINT = "/order";

    private final KakaoPayProperties properties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;
    private final KakaoPayHeadersProvider headersProvider;
    private final KakaoStatusResponseParser responseParser;
    private final KakaoStatusErrorHandler errorHandler;

    public KakaoStatusResponse status(String tid) {
        validateRequired(tid, "tid");

        try {
            ResponseEntity<Map<String, Object>> response = executeRequest(tid);
            return parseResponse(tid, response);
        } catch (HttpClientErrorException e) {
            throw errorHandler.handleHttpClientError(e, tid);
        } catch (RestClientException e) {
            throw errorHandler.handleRestClientError(e, tid);
        }
    }

    private ResponseEntity<Map<String, Object>> executeRequest(String tid) {
        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(createStatusRequestBody(tid), headersProvider.createJsonHeaders());

        return restTemplate.exchange(
                KAKAO_PAY_API_URL + STATUS_ENDPOINT,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
    }

    private KakaoStatusResponse parseResponse(String tid, ResponseEntity<Map<String, Object>> response) {
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new ApiException(
                    ErrorCode.PAYMENT_PROVIDER_ERROR,
                    "KakaoPay status API failed: status=" + response.getStatusCode()
            );
        }

        KakaoStatusResponse result = responseParser.parse(response.getBody());
        log.debug("KakaoPay status success: tid={}, status={}", 
                SensitiveDataMasker.maskPaymentKey(tid), result.status());
        return result;
    }

    private Map<String, Object> createStatusRequestBody(String tid) {
        validateRequired(properties.getCid(), "cid");

        Map<String, Object> body = new HashMap<>();
        body.put("cid", properties.getCid());
        body.put("tid", tid);
        return body;
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
    }
}