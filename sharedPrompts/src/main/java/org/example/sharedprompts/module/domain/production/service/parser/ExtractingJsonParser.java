package org.example.sharedprompts.module.domain.production.service.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.exception.ParseException;
import org.example.sharedprompts.module.domain.production.service.validator.ResponseValidator;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON 추출 및 파싱 전략
 * 텍스트에서 JSON을 추출한 후 파싱 수행
 * 마크다운 코드 블록이나 텍스트 내부의 JSON 객체/배열을 찾아서 추출
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExtractingJsonParser implements Parser {

    private final ObjectMapper objectMapper;
    
    // 마크다운 코드 블록 패턴: ```json ... ``` 또는 ``` ... ```
    private static final Pattern MARKDOWN_JSON_BLOCK = Pattern.compile(
        "```(?:json)?\\s*\\n?(.*?)\\s*```", 
        Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );
    
    // JSON 객체 패턴: { ... }
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile(
        "\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}", 
        Pattern.DOTALL
    );
    
    // JSON 배열 패턴: [ ... ]
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile(
        "\\[[^\\[\\]]*(?:\\[[^\\[\\]]*\\][^\\[\\]]*)*\\]", 
        Pattern.DOTALL
    );

    @Override
    public ParsedResponse parse(String rawResponse, ResponseValidator validator) {
        try {
            String extractedJson = extractJson(rawResponse);
            log.debug("Extracted JSON from text: {}", extractedJson);
            
            JsonNode jsonNode = objectMapper.readTree(extractedJson);
            validator.validate(jsonNode);
            String parsedJson = objectMapper.writeValueAsString(jsonNode);
            return new ParsedResponse(parsedJson, jsonNode);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ParseException("Failed to parse extracted JSON: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParseException("Failed to extract or parse JSON: " + e.getMessage(), e);
        }
    }
    
    /**
     * 텍스트에서 JSON을 추출
     * 1. 마크다운 코드 블록에서 추출 시도
     * 2. JSON 객체 패턴 매칭 시도
     * 3. JSON 배열 패턴 매칭 시도
     * 4. 실패 시 원본 텍스트 반환
     */
    private String extractJson(String text) {
        if (text == null || text.isBlank()) {
            throw new ParseException("Text is null or empty");
        }
        
        // 1. 마크다운 코드 블록에서 JSON 추출 시도
        Matcher markdownMatcher = MARKDOWN_JSON_BLOCK.matcher(text);
        if (markdownMatcher.find()) {
            String extracted = markdownMatcher.group(1).trim();
            if (isValidJson(extracted)) {
                log.debug("Extracted JSON from markdown code block");
                return extracted;
            }
        }
        
        // 2. JSON 객체 패턴 찾기
        Matcher objectMatcher = JSON_OBJECT_PATTERN.matcher(text);
        while (objectMatcher.find()) {
            String candidate = objectMatcher.group(0).trim();
            if (isValidJson(candidate)) {
                log.debug("Extracted JSON object from text");
                return candidate;
            }
        }
        
        // 3. JSON 배열 패턴 찾기
        Matcher arrayMatcher = JSON_ARRAY_PATTERN.matcher(text);
        while (arrayMatcher.find()) {
            String candidate = arrayMatcher.group(0).trim();
            if (isValidJson(candidate)) {
                log.debug("Extracted JSON array from text");
                return candidate;
            }
        }
        
        // 4. 더 정교한 JSON 추출: 중첩된 구조를 고려한 추출
        String nestedJson = extractNestedJson(text);
        if (nestedJson != null && isValidJson(nestedJson)) {
            log.debug("Extracted nested JSON from text");
            return nestedJson;
        }
        
        // 5. 실패 시 원본 반환 (이전 파서들이 실패했으므로 여기서도 실패할 가능성이 높음)
        log.warn("Could not extract JSON from text, returning original");
        return text.trim();
    }
    
    /**
     * 중첩된 JSON 구조 추출 (재귀적으로 매칭)
     * 텍스트에서 첫 번째 유효한 JSON 객체나 배열을 찾아서 반환
     */
    private String extractNestedJson(String text) {
        // 객체와 배열 중 먼저 나오는 것을 찾기
        int objectIdx = text.indexOf('{');
        int arrayIdx = text.indexOf('[');
        
        // 둘 다 없으면 null 반환
        if (objectIdx == -1 && arrayIdx == -1) {
            return null;
        }
        
        // 객체와 배열 중 먼저 나오는 것 선택
        if (objectIdx != -1 && (arrayIdx == -1 || objectIdx < arrayIdx)) {
            String extracted = extractBalanced(text, objectIdx, '{', '}');
            if (extracted != null && isValidJson(extracted)) {
                return extracted;
            }
        }
        
        if (arrayIdx != -1) {
            String extracted = extractBalanced(text, arrayIdx, '[', ']');
            if (extracted != null && isValidJson(extracted)) {
                return extracted;
            }
        }
        
        return null;
    }
    
    /**
     * 균형잡힌 괄호로 둘러싸인 문자열 추출
     * JSON 문자열 내부의 괄호는 무시하도록 처리
     */
    private String extractBalanced(String text, int startIdx, char open, char close) {
        int depth = 0;
        int endIdx = startIdx;
        boolean inString = false;
        boolean escapeNext = false;
        
        for (int i = startIdx; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (escapeNext) {
                // 이전 문자가 백슬래시였으므로 이 문자는 이스케이프된 문자
                escapeNext = false;
                continue;
            }
            
            if (c == '\\') {
                // 다음 문자가 이스케이프될 것임을 표시
                escapeNext = true;
                continue;
            }
            
            if (c == '"') {
                // 이스케이프되지 않은 따옴표만 문자열 시작/종료로 처리
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == open) {
                    depth++;
                } else if (c == close) {
                    depth--;
                    if (depth == 0) {
                        endIdx = i + 1;
                        break;
                    }
                }
            }
        }
        
        if (depth == 0 && endIdx > startIdx) {
            return text.substring(startIdx, endIdx).trim();
        }
        
        return null;
    }
    
    /**
     * 문자열이 유효한 JSON인지 빠르게 검증
     */
    private boolean isValidJson(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        
        String trimmed = candidate.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        
        // 기본적인 JSON 구조 확인
        boolean startsWithJson = trimmed.startsWith("{") || trimmed.startsWith("[");
        boolean endsWithJson = trimmed.endsWith("}") || trimmed.endsWith("]");
        
        if (!startsWithJson || !endsWithJson) {
            return false;
        }
        
        // 실제 파싱 가능한지 확인
        try {
            objectMapper.readTree(trimmed);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

