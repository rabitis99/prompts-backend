package org.example.sharedprompts.module.domain.production.service.literary.novel;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChapterPlan {

    private final int chapterCount;
    private final List<String> chapterTitlesOrPrompts;
    private final String userContext;
}
