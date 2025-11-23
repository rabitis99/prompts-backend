package org.example.sharedprompts.global.response;


import lombok.Getter;

import java.util.List;

@Getter
public class PageResponse<T> {
    // Getter
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;

    public PageResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
    }

}
