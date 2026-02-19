package org.example.sharedprompts.module.domain.production.service.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.exception.ParseException;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.service.validator.ResponseValidator;
import org.example.sharedprompts.module.domain.production.service.validator.ValidatorFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
    private final ExtractingJsonParser extractingJsonParser;
    private final ValidatorFactory validatorFactory;
    private final ObjectMapper objectMapper;

    /**
     * AI 응답 파싱
     * strictParse → relaxedParse → extractingParse 순서로 fallback 로직 수행
     */
    public ParsedResponse parseResponse(String rawResponse, ProductionCommandType commandType) {
        log.info("Parsing AI response - commandType: {}, responseLength: {}", 
                commandType, rawResponse != null ? rawResponse.length() : 0);
        log.info(rawResponse);

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new ParseException("Raw response is null or empty");
        }

        // JSON 형식인지 확인
        String trimmedResponse = rawResponse.trim();
        boolean isJson = (trimmedResponse.startsWith("{") && trimmedResponse.endsWith("}")) ||
                         (trimmedResponse.startsWith("[") && trimmedResponse.endsWith("]"));
        
        // JSON이 아닌 경우 처리
        if (!isJson) {
            // 이미지 타입인 경우 파일 경로를 JSON으로 래핑
            if (commandType == ProductionCommandType.IMAGE) {
                log.info("Detected image file path, wrapping as JSON - commandType: {}", commandType);
                String wrappedJson = wrapAsJson("content", rawResponse);
                trimmedResponse = wrappedJson;
            }
            // EMAIL 타입인 경우 subject와 body 필드로 변환
            else if (commandType == ProductionCommandType.EMAIL) {
                log.info("Detected non-JSON email response, converting to email format - commandType: {}", commandType);
                String wrappedJson = wrapAsEmailFormat(rawResponse);
                trimmedResponse = wrappedJson;
            }
            // 그 외 타입(BLOG, DOCUMENT 등)에서 JSON이 아닌 경우 텍스트/마크다운으로 간주하고 래핑
            else {
                log.info("Detected non-JSON response (likely text/markdown), wrapping as JSON - commandType: {}", commandType);
                String wrappedJson = wrapAsJson("content", rawResponse);
                trimmedResponse = wrappedJson;
            }
        }

        ResponseValidator validator = validatorFactory.getValidator(commandType);

        try {
            return strictJsonParser.parse(trimmedResponse, validator);
        } catch (ParseException e) {
            log.warn("Strict parsing failed, attempting relaxed parsing - commandType: {}", commandType, e);
            try {
                return relaxedJsonParser.parse(trimmedResponse, validator);
            } catch (Exception e2) {
                log.warn("Relaxed parsing also failed, attempting JSON extraction - commandType: {}", commandType, e2);
                try {
                    return extractingJsonParser.parse(trimmedResponse, validator);
                } catch (Exception e3) {
                    // JSON 추출도 실패한 경우, 타입에 맞게 래핑하여 재시도
                    log.warn("JSON extraction failed, attempting to wrap as JSON - commandType: {}", commandType);
                    try {
                        String wrappedJson;
                        if (commandType == ProductionCommandType.EMAIL) {
                            wrappedJson = wrapAsEmailFormat(rawResponse);
                        } else if (commandType == ProductionCommandType.IMAGE) {
                            wrappedJson = wrapAsJson("content", rawResponse);
                        } else {
                            wrappedJson = wrapAsJson("content", rawResponse);
                        }
                        return strictJsonParser.parse(wrappedJson, validator);
                    } catch (Exception e4) {
                        log.error("All parsing attempts failed - commandType: {}", commandType, e4);
                        throw new ParseException("All parsing attempts failed: " + e.getMessage(), e);
                    }
                }
            }
        }
    }
    
    /**
     * 마크다운 형식인지 확인
     */
    private boolean isMarkdownFormat(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        
        // 마크다운 특징: **, ##, -, *, ` 등으로 시작하거나 포함
        boolean hasMarkdownFeatures = text.contains("**") || 
                                      text.contains("##") || 
                                      text.contains("###") ||
                                      text.startsWith("#") ||
                                      text.contains("```") ||
                                      (text.contains("- ") && text.contains("\n")) ||
                                      (text.contains("* ") && text.contains("\n"));
        
        // JSON 형식이 아니면서 마크다운 특징이 있으면 마크다운으로 간주
        return hasMarkdownFeatures && !text.trim().startsWith("{") && !text.trim().startsWith("[");
    }
    
    /**
     * 텍스트를 JSON으로 래핑 (일반적인 방법)
     */
    private String wrapAsJson(String fieldName, String text) {
        try {
            // ObjectMapper를 사용하여 안전하게 JSON으로 변환
            Map<String, String> jsonMap = new HashMap<>();
            jsonMap.put(fieldName, text);
            return objectMapper.writeValueAsString(jsonMap);
        } catch (Exception e) {
            log.warn("Failed to wrap text as JSON using ObjectMapper, using manual escape", e);
            // Fallback: 수동 이스케이프 (순서 중요: \를 먼저 처리)
            String escaped = text
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            return String.format("{\"%s\": \"%s\"}", fieldName, escaped);
        }
    }
    
    /**
     * 마크다운 텍스트를 EMAIL 형식으로 변환
     * subject와 body 필드를 추출하거나 전체를 body로 사용
     */
    private String wrapAsEmailFormat(String text) {
        try {
            // 마크다운에서 제목 추출 시도
            String subject = extractEmailSubject(text);
            String body = text; // 전체를 body로 사용
            
            Map<String, String> jsonMap = new HashMap<>();
            jsonMap.put("subject", subject);
            jsonMap.put("body", body);
            return objectMapper.writeValueAsString(jsonMap);
        } catch (Exception e) {
            log.warn("Failed to wrap email as JSON using ObjectMapper, using manual escape", e);
            // Fallback: 수동 이스케이프
            String subject = extractEmailSubject(text);
            String escapedBody = text
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            String escapedSubject = subject
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            return String.format("{\"subject\": \"%s\", \"body\": \"%s\"}", escapedSubject, escapedBody);
        }
    }
    
    /**
     * 마크다운 텍스트에서 이메일 제목 추출
     * ### 제목, **제목**, 또는 첫 번째 줄을 제목으로 사용
     */
    private String extractEmailSubject(String text) {
        if (text == null || text.isBlank()) {
            return "No Subject";
        }
        
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            // ### 제목 형식
            if (trimmed.startsWith("### ")) {
                return trimmed.substring(4).trim();
            }
            // ## 제목 형식
            if (trimmed.startsWith("## ")) {
                return trimmed.substring(3).trim();
            }
            // # 제목 형식
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
            // **제목** 형식
            if (trimmed.startsWith("**") && trimmed.endsWith("**")) {
                return trimmed.substring(2, trimmed.length() - 2).trim();
            }
            // 제목: 형식
            if (trimmed.toLowerCase().startsWith("제목:") || trimmed.toLowerCase().startsWith("subject:")) {
                int colonIndex = trimmed.indexOf(':');
                if (colonIndex > 0) {
                    return trimmed.substring(colonIndex + 1).trim();
                }
            }
        }
        
        // 제목을 찾지 못한 경우 첫 번째 비어있지 않은 줄을 제목으로 사용
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && trimmed.length() < 100) {
                // 마크다운 문법 제거
                String cleaned = trimmed
                        .replaceAll("^#+\\s*", "")
                        .replaceAll("\\*\\*", "")
                        .replaceAll("\\*", "")
                        .trim();
                if (!cleaned.isEmpty()) {
                    return cleaned;
                }
            }
        }
        
        // 모든 시도 실패 시 기본값
        return "No Subject";
    }
    
    /**
     * 마크다운 텍스트를 JSON으로 래핑 (하위 호환성을 위해 유지)
     */
    private String wrapMarkdownAsJson(String markdownText) {
        return wrapAsJson("content", markdownText);
    }
}

