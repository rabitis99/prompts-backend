package org.example.sharedprompts.domain.payment.application.command.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.application.command.service.metadata.PaymentMetadataBuilder;
import org.example.sharedprompts.domain.payment.application.command.service.metadata.PaymentMetadataExtractor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentMetadataService {

    private final PaymentMetadataExtractor extractor;
    private final PaymentMetadataBuilder builder;

    public String extractProductName(String metadata) {
        return extractor.extractProductName(metadata);
    }

    public String addRedirectUrlToMetadata(String existingMetadata, String redirectUrl, String tid) {
        return builder.addRedirectUrl(existingMetadata, redirectUrl, tid);
    }
}

