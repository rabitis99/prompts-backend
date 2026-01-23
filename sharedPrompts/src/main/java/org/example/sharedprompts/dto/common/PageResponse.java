package org.example.sharedprompts.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class PageResponse<T> {
    // Getter
    private final List<T> content;
    private final int page;
    private final int size;

    @JsonProperty("total_elements")
    private final long totalElements;

    @JsonProperty("total_pages")
    private final Integer totalPages;

    private final Boolean last;

    public static <T> PageResponse<T> of(Page<T> page) {
        if (page == null) {
            return PageResponse.<T>builder()
                    .content(List.of())
                    .page(0)
                    .size(0)
                    .totalElements(0)
                    .totalPages(null)
                    .last(null)
                    .build();
        }
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

}

