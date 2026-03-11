package org.example.sharedprompts.domain.prompt.application.port.in.query;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.sort.SortType;

/** 프롬프트 검색 쿼리 */
public record SearchPromptsQuery(
        int page,
        int size,
        SortType sort,
        PromptCategory category,
        Long ownerId,
        Long viewerId,
        String keyword
) {

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 빌더로 생성 시 인자 순서/타입 실수를 줄이고 의도를 드러낸다.
     * 특히 ownerId/viewerId는 타입이 같아 positional 생성 시 바꿔도 컴파일 통과하므로
     * 명명된 setter 사용을 권장한다.
     */
    public static final class Builder {
        private int page;
        private int size;
        private SortType sort;
        private PromptCategory category;
        private Long ownerId;
        private Long viewerId;
        private String keyword;

        private Builder() {}

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder sort(SortType sort) {
            this.sort = sort;
            return this;
        }

        public Builder category(PromptCategory category) {
            this.category = category;
            return this;
        }

        public Builder ownerId(Long ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder viewerId(Long viewerId) {
            this.viewerId = viewerId;
            return this;
        }

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public SearchPromptsQuery build() {
            return new SearchPromptsQuery(page, size, sort, category, ownerId, viewerId, keyword);
        }
    }
}

