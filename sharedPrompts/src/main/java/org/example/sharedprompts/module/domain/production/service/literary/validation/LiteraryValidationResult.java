package org.example.sharedprompts.module.domain.production.service.literary.validation;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class LiteraryValidationResult {

    private final boolean valid;
    @Builder.Default
    private final List<String> errors = Collections.emptyList();

    public static LiteraryValidationResult ok() {
        return LiteraryValidationResult.builder().valid(true).build();
    }

    public static LiteraryValidationResult failure(List<String> errors) {
        return LiteraryValidationResult.builder()
                .valid(false)
                .errors(errors != null ? errors : Collections.emptyList())
                .build();
    }
}
