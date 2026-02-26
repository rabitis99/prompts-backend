package org.example.sharedprompts.domain.prompt.domain.model;

/**
 * 출력 계약 — 프롬프트 결과물의 예상 형식과 스키마를 정의한다.
 * EXTRACTION Objective에서 Constrained Decoding과 함께 사용된다.
 */
public final class OutputContract {

    private final OutputFormat format;
    private final String jsonSchema;   // null이면 비구조화 텍스트
    private final Integer maxTokens;

    private OutputContract(OutputFormat format, String jsonSchema, Integer maxTokens) {
        this.format = format;
        this.jsonSchema = jsonSchema;
        this.maxTokens = maxTokens;
    }

    public static OutputContract freeText(Integer maxTokens) {
        return new OutputContract(OutputFormat.FREE_TEXT, null, maxTokens);
    }

    public static OutputContract jsonStructured(String jsonSchema, Integer maxTokens) {
        if (jsonSchema == null || jsonSchema.isBlank()) {
            throw new IllegalArgumentException("JSON Schema는 비어있을 수 없습니다.");
        }
        return new OutputContract(OutputFormat.JSON_STRUCTURED, jsonSchema, maxTokens);
    }

    public static OutputContract markdown(Integer maxTokens) {
        return new OutputContract(OutputFormat.MARKDOWN, null, maxTokens);
    }

    public OutputFormat getFormat() { return format; }
    public String getJsonSchema() { return jsonSchema; }
    public Integer getMaxTokens() { return maxTokens; }
    public boolean hasJsonSchema() { return jsonSchema != null && !jsonSchema.isBlank(); }

    public enum OutputFormat {
        FREE_TEXT,
        JSON_STRUCTURED,
        MARKDOWN,
        BULLETED_LIST
    }
}
