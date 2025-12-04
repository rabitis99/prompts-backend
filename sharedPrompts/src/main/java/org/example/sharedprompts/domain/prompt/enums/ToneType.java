package org.example.sharedprompts.domain.prompt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ToneType {
    FRIENDLY("친근한"),
    FORMAL("공식적인"),
    HUMOROUS("유머러스한"),
    MOTIVATIONAL("동기부여형"),
    CASUAL("일상적인"),
    PROFESSIONAL("전문적인"),
    EMPATHETIC("공감하는"),
    SARCASTIC("비꼬는"),
    POSITIVE("긍정적인"),
    NEGATIVE("부정적인"),
    INSPIRATIONAL("영감을 주는"),
    NEUTRAL("중립적인");

    private final String description;
}
