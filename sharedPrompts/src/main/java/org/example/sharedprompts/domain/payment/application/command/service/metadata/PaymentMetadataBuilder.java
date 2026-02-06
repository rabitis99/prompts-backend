package org.example.sharedprompts.domain.payment.application.command.service.metadata;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static org.example.sharedprompts.domain.payment.application.command.service.metadata.PaymentMetadataKeys.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMetadataBuilder {

    private final PaymentMetadataParser metadataParser;

    public String addRedirectUrl(String existingMetadata, String redirectUrl, String tid) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            log.warn("redirectUrl이 null이거나 비어있음");
            throw new IllegalArgumentException("redirectUrl은 필수입니다");
        }

        Map<String, Object> metadataMap = metadataParser.parseMetadata(existingMetadata)
                .orElse(new HashMap<>());

        metadataMap.put(NEXT_REDIRECT_PC_URL, redirectUrl);
        metadataMap.put(REDIRECT_URL, redirectUrl);

        if (tid != null && !tid.isEmpty()) {
            metadataMap.put(TID, tid);
        }

        try {
            return metadataParser.serializeMetadata(metadataMap);
        } catch (MetadataSerializationException e) {
            log.error("메타데이터 직렬화 실패, fallback 생성 시도: redirectUrl={}, tid={}",
                    redirectUrl, tid, e);
            return createFallbackMetadata(redirectUrl, tid);
        }
    }

    public String createMetadata(Map<String, String> keyValuePairs) {
        Map<String, Object> metadataMap = new HashMap<>(keyValuePairs);
        return metadataParser.serializeMetadata(metadataMap);
    }

    private String createFallbackMetadata(String redirectUrl, String tid) {
        Map<String, String> fallbackMap = new HashMap<>();
        fallbackMap.put(NEXT_REDIRECT_PC_URL, redirectUrl);
        fallbackMap.put(REDIRECT_URL, redirectUrl);

        if (tid != null && !tid.isEmpty()) {
            fallbackMap.put(TID, tid);
        }

        try {
            return metadataParser.serializeMetadata(new HashMap<>(fallbackMap));
        } catch (MetadataSerializationException e) {
            log.error("fallback 메타데이터 생성도 실패: redirectUrl={}, tid={}",
                    redirectUrl, tid, e);
            // 최소한의 유효한 JSON 반환 (JSON 이스케이프 적용)
            String escapedRedirectUrl = escapeJson(redirectUrl);
            if (tid != null && !tid.isEmpty()) {
                String escapedTid = escapeJson(tid);
                return String.format("{\"%s\":\"%s\",\"%s\":\"%s\",\"%s\":\"%s\"}",
                        REDIRECT_URL, escapedRedirectUrl,
                        NEXT_REDIRECT_PC_URL, escapedRedirectUrl,
                        TID, escapedTid);
            } else {
                return String.format("{\"%s\":\"%s\",\"%s\":\"%s\"}",
                        REDIRECT_URL, escapedRedirectUrl,
                        NEXT_REDIRECT_PC_URL, escapedRedirectUrl);
            }
        }
    }

    /**
     * JSON 문자열 값에 대한 이스케이프 처리
     * 특수문자(따옴표, 백슬래시 등)를 JSON 형식에 맞게 이스케이프합니다.
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return new String(JsonStringEncoder.getInstance().quoteAsString(value));
    }
}

