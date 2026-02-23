package org.example.sharedprompts.module.domain.production.service.literary.novel;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class ChapterPlan {

    private final int chapterCount;
    @Builder.Default
    private final List<String> chapterTitlesOrPrompts = Collections.emptyList();
    private final String userContext;
}
