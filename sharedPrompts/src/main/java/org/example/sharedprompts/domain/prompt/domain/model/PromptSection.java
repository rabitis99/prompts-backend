package org.example.sharedprompts.domain.prompt.domain.model;

/**
 * 프롬프트 섹션 — 역할, 목적, 제약, 출력 형식 등 구조적 단위.
 */
public final class PromptSection {

    private final SectionType type;
    private final String content;
    private final boolean required;

    private PromptSection(SectionType type, String content, boolean required) {
        this.type = type;
        this.content = content;
        this.required = required;
    }

    public static PromptSection required(SectionType type, String content) {
        return new PromptSection(type, content, true);
    }

    public static PromptSection optional(SectionType type, String content) {
        return new PromptSection(type, content, false);
    }

    public SectionType getType() { return type; }
    public String getContent() { return content; }
    public boolean isRequired() { return required; }

    public enum SectionType {
        ROLE,
        CONTEXT,
        INSTRUCTION,
        OUTPUT_FORMAT,
        CONSTRAINTS,
        FEW_SHOT_EXAMPLES,
        VERIFICATION_CHECKLIST
    }
}
