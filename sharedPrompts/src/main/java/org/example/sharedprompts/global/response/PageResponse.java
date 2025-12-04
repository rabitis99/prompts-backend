package org.example.sharedprompts.global.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageResponse<T> {
    // Getter
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;

    public PageResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
    }
    public static <T> PageResponse<T> of(Page<T> page) {
        if (page == null) {
            return new PageResponse<>(List.of(), 0, 0, 0);
        }
        return new PageResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements()
        );
    }

}
