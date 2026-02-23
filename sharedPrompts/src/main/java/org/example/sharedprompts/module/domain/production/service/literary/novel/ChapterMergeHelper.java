package org.example.sharedprompts.module.domain.production.service.literary.novel;

import java.util.List;

public final class ChapterMergeHelper {

    private static final String CHAPTER_SEPARATOR = "\n\n";

    private ChapterMergeHelper() {
    }

    public static String mergeChapters(List<String> chapterContents) {
        if (chapterContents == null || chapterContents.isEmpty()) {
            return "";
        }
        List<String> nonNull = chapterContents.stream()
                .filter(c -> c != null)
                .toList();
        return String.join(CHAPTER_SEPARATOR, nonNull);
    }
}
