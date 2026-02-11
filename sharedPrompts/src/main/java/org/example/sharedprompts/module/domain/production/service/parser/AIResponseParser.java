package org.example.sharedprompts.module.domain.production.service.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.exception.ParseException;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.service.validator.ResponseValidator;
import org.example.sharedprompts.module.domain.production.service.validator.ValidatorFactory;
import org.springframework.stereotype.Service;

/**
 * AI 응답 파싱 오케스트레이션 서비스
 * Parsing orchestration과 strict/relaxed fallback 로직, 로깅만 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIResponseParser {

    private final StrictJsonParser strictJsonParser;
    private final RelaxedJsonParser relaxedJsonParser;
    private final ValidatorFactory validatorFactory;

    /**
     * AI 응답 파싱
     * strictParse → relaxedParse 순서로 fallback 로직 수행
     */
    public ParsedResponse parseResponse(String rawResponse, ProductionCommandType commandType) {
        log.info("Parsing AI response - commandType: {}, responseLength: {}", 
                commandType, rawResponse != null ? rawResponse.length() : 0);

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new ParseException("Raw response is null or empty");
        }

        ResponseValidator validator = validatorFactory.getValidator(commandType);

        try {
            return strictJsonParser.parse(rawResponse, validator);
        } catch (ParseException e) {
            log.warn("Strict parsing failed, attempting relaxed parsing - commandType: {}", commandType, e);
            try {
                return relaxedJsonParser.parse(rawResponse, validator);
            } catch (Exception e2) {
                log.error("Relaxed parsing also failed - commandType: {}", commandType, e2);
                throw new ParseException("All parsing attempts failed: " + e.getMessage(), e);
            }
        }
    }
}

