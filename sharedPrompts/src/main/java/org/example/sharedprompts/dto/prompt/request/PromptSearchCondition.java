package org.example.sharedprompts.dto.prompt.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.enums.SortType;

@Getter
@Setter
@NoArgsConstructor
public class PromptSearchCondition {

    private int page = 0;
    private int size = 20;
    private SortType sort = SortType.LATEST;
    private PromptCategory promptCategory;

    public static PromptSearchCondition of(int page, int size, SortType sort, PromptCategory category) {
        PromptSearchCondition condition = new PromptSearchCondition();
        condition.setPage(page);
        condition.setSize(size);
        condition.setSort(sort);
        condition.setPromptCategory(category);
        return condition;
    }
}