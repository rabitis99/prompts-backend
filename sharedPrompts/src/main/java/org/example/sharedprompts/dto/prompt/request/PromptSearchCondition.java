package org.example.sharedprompts.dto.prompt.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.enums.SortType;

@Getter
@Setter
@NoArgsConstructor
public class PromptSearchCondition {

    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하로 입력해주세요.")
    private int size = 20;
    private SortType sort = SortType.LATEST;

    @JsonProperty("prompt_category")
    private PromptCategory promptCategory;

    /**
     * @ModelAttribute 바인딩을 위한 snake_case 파라미터 지원
     * Spring은 쿼리 파라미터 "prompt_category"를 이 메서드로 바인딩합니다.
     *
     * null 값 처리:
     * - 쿼리 파라미터가 없거나 null이면 null로 설정되어 필터링 없이 모든 카테고리를 조회합니다.
     * - Repository의 applyCategory() 메서드에서 null 체크를 통해 필터링을 건너뜁니다.
     */
    public void setPrompt_category(PromptCategory promptCategory) {
        this.promptCategory = promptCategory;
    }

    public static PromptSearchCondition of(int page, int size, SortType sort, PromptCategory category) {
        PromptSearchCondition condition = new PromptSearchCondition();
        condition.setPage(page);
        condition.setSize(size);
        condition.setSort(sort);
        condition.setPromptCategory(category);
        return condition;
    }
}