package org.example.sharedprompts.module.domain.production.service.job.process.execution.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.exception.ParseException;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.AIServiceException;
import org.example.sharedprompts.module.domain.production.service.parser.AIResponseParser;
import org.example.sharedprompts.module.domain.production.service.parser.ParsedResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIResponseHandler {

    private final AIResponseParser aiResponseParser;

    public ParsedResponse parse(String rawResponse, ProductionCommandType commandType) {
        log.info("Parsing AI response - commandType: {}, responseLength: {}",
                commandType, rawResponse != null ? rawResponse.length() : 0);

        try {
            return aiResponseParser.parseResponse(rawResponse, commandType);
        } catch (ParseException e) {
            log.error("Failed to parse AI response - commandType: {}", commandType, e);
            throw new AIServiceException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }
}

