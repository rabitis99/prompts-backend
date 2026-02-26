package org.example.sharedprompts.domain.prompt.domain.model;

import java.util.List;

/**
 * 프롬프트 생성 제약 조건.
 */
public final class Constraints {

    private final Integer minLength;
    private final Integer maxLength;
    private final List<String> requiredKeywords;
    private final List<String> prohibitedKeywords;
    private final boolean requireStepByStep;
    private final boolean requireCitations;

    private Constraints(Builder builder) {
        this.minLength = builder.minLength;
        this.maxLength = builder.maxLength;
        this.requiredKeywords = builder.requiredKeywords != null
                ? List.copyOf(builder.requiredKeywords)
                : List.of();
        this.prohibitedKeywords = builder.prohibitedKeywords != null
                ? List.copyOf(builder.prohibitedKeywords)
                : List.of();
        this.requireStepByStep = builder.requireStepByStep;
        this.requireCitations = builder.requireCitations;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Constraints defaults() {
        return builder().maxLength(2000).build();
    }

    public Integer getMinLength() { return minLength; }
    public Integer getMaxLength() { return maxLength; }
    public List<String> getRequiredKeywords() { return requiredKeywords; }
    public List<String> getProhibitedKeywords() { return prohibitedKeywords; }
    public boolean isRequireStepByStep() { return requireStepByStep; }
    public boolean isRequireCitations() { return requireCitations; }

    public static final class Builder {
        private Integer minLength;
        private Integer maxLength;
        private List<String> requiredKeywords;
        private List<String> prohibitedKeywords;
        private boolean requireStepByStep;
        private boolean requireCitations;

        public Builder minLength(Integer minLength) { this.minLength = minLength; return this; }
        public Builder maxLength(Integer maxLength) { this.maxLength = maxLength; return this; }
        public Builder requiredKeywords(List<String> keywords) { this.requiredKeywords = keywords; return this; }
        public Builder prohibitedKeywords(List<String> keywords) { this.prohibitedKeywords = keywords; return this; }
        public Builder requireStepByStep(boolean v) { this.requireStepByStep = v; return this; }
        public Builder requireCitations(boolean v) { this.requireCitations = v; return this; }

        public Constraints build() { return new Constraints(this); }
    }
}
