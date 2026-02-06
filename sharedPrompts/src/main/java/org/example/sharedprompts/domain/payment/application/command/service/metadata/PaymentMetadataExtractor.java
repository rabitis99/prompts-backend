package org.example.sharedprompts.domain.payment.application.command.service.metadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMetadataExtractor {

    private final PaymentMetadataParser metadataParser;

    public String extractProductName(String metadata) {
        if (metadata == null || metadata.isEmpty()) {
            log.debug("메타데이터가 null이거나 비어있어 기본값 반환");
            return PaymentMetadataKeys.getDefaultProductName();
        }

        return metadataParser.parseMetadata(metadata)
                .map(map -> {
                    String productName = extractStringValue(map, PaymentMetadataKeys.PRODUCT_NAME);
                    if (!productName.isEmpty()) {
                        log.debug("메타데이터에서 상품명 추출 성공: productName={}", productName);
                        return productName;
                    }
                    log.debug("메타데이터에 상품명이 없어 기본값 반환");
                    return PaymentMetadataKeys.getDefaultProductName();
                })
                .orElseGet(() -> {
                    log.debug("메타데이터 파싱 실패로 기본값 반환");
                    return PaymentMetadataKeys.getDefaultProductName();
                });
    }

    public Optional<String> extractValue(String metadata, String key) {
        return metadataParser.parseMetadata(metadata)
                .map(map -> extractStringValue(map, key))
                .filter(value -> !value.isBlank());
    }

    private String extractStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
}

