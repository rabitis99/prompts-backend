package org.example.sharedprompts.domain.prompt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExperienceLevel {
    JUNIOR("초급"),
    MID("중급"),
    SENIOR("전문가");

    private final String description;
}
