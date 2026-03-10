package org.example.sharedprompts.domain.prompt.application.semantic.resolution;

import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 시맨틱 해석 전 category·request_mode 제약 검증 */
@Component
public class SemanticRequestModeValidator {

    public List<String> validateForResolution(PromptCategory category) {
        List<String> errors = new ArrayList<>();
        if (category == null) {
            errors.add("category is required for SIMPLE/ADVANCED");
            return errors;
        }
        if (category == PromptCategory.EXTRACTION) {
            errors.add("EXTRACTION category is only valid with request_mode=EXTRACTION");
        }
        return errors;
    }
}
