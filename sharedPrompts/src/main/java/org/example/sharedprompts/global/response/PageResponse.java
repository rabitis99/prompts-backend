package org.example.sharedprompts.global.response;

import lombok.Getter;

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

}
