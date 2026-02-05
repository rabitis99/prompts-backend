package org.example.sharedprompts.domain.payment.application.command.service.metadata;

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
            // 최소한의 유효한 JSON 반환
            return String.format("{\"%s\":\"%s\",\"%s\":\"%s\"}",
                    REDIRECT_URL, redirectUrl,
                    NEXT_REDIRECT_PC_URL, redirectUrl);
        }
    }
}

