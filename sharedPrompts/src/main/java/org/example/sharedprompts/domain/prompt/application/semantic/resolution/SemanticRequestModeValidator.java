package org.example.sharedprompts.domain.prompt.application.semantic.resolution;

import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.RequestMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 시맨틱 해석 전 category·request_mode 조합 제약 검증 */
@Component
public class SemanticRequestModeValidator {

    /**
     * SIMPLE/ADVANCED일 때 category 필수, EXTRACTION category는 request_mode=EXTRACTION일 때만 허용.
     */
    public List<String> validateForResolution(PromptCategory category, RequestMode requestMode) {
        List<String> errors = new ArrayList<>();
        if (requestMode == RequestMode.SIMPLE || requestMode == RequestMode.ADVANCED) {
            if (category == null) {
                errors.add("category is required for SIMPLE/ADVANCED");
                return errors;
            }
            if (category == PromptCategory.EXTRACTION) {
                errors.add("EXTRACTION category is only valid with request_mode=EXTRACTION");
            }
        }
        return errors;
    }
}
