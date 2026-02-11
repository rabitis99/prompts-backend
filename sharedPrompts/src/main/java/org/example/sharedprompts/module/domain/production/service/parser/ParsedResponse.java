package org.example.sharedprompts.module.domain.production.service.parser;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 파싱된 응답 결과
 */
public record ParsedResponse(
        String jsonString,
        JsonNode jsonNode
) {}

