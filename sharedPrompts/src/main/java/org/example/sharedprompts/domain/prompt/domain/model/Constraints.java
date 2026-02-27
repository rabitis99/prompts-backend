package org.example.sharedprompts.domain.prompt.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * 프롬프트 생성 제약 조건.
 */
public final class Constraints {

    private static final int DEFAULT_MAX_LENGTH = 2000;

    private final Integer minLength;
    private final Integer maxLength;
    private final List<String> requiredKeywords;
    private final List<String> prohibitedKeywords;
    private final boolean requireStepByStep;
    private final boolean requireCitations;

    private Constraints(Builder builder) {
        this.minLength = builder.minLength;
        this.maxLength = builder.maxLength;
        if (this.minLength != null && this.minLength < 0) {
            throw new IllegalArgumentException("minLength cannot be negative");
        }
        if (this.maxLength != null && this.maxLength < 0) {
            throw new IllegalArgumentException("maxLength cannot be negative");
        }
        if (this.minLength != null && this.maxLength != null && this.minLength > this.maxLength) {
            throw new IllegalArgumentException("minLength cannot be greater than maxLength");
        }
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

    /**
     * 기본 제약 조건을 반환한다.
     * <ul>
     *   <li>maxLength: 2000</li>
     *   <li>minLength: null (제한 없음)</li>
     * </ul>
     *
     * @return 기본 {@link Constraints} 인스턴스
     */
    public static Constraints defaults() {
        return builder().maxLength(DEFAULT_MAX_LENGTH).build();
    }

    public Integer getMinLength() { return minLength; }
    public Integer getMaxLength() { return maxLength; }
    public List<String> getRequiredKeywords() { return requiredKeywords; }
    public List<String> getProhibitedKeywords() { return prohibitedKeywords; }
    public boolean isRequireStepByStep() { return requireStepByStep; }
    public boolean isRequireCitations() { return requireCitations; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Constraints that = (Constraints) o;
        return requireStepByStep == that.requireStepByStep &&
                requireCitations == that.requireCitations &&
                Objects.equals(minLength, that.minLength) &&
                Objects.equals(maxLength, that.maxLength) &&
                Objects.equals(requiredKeywords, that.requiredKeywords) &&
                Objects.equals(prohibitedKeywords, that.prohibitedKeywords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minLength, maxLength, requiredKeywords,
                prohibitedKeywords, requireStepByStep, requireCitations);
    }

    @Override
    public String toString() {
        return "Constraints{" +
                "minLength=" + minLength +
                ", maxLength=" + maxLength +
                ", requiredKeywords=" + requiredKeywords +
                ", prohibitedKeywords=" + prohibitedKeywords +
                ", requireStepByStep=" + requireStepByStep +
                ", requireCitations=" + requireCitations +
                '}';
    }

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
