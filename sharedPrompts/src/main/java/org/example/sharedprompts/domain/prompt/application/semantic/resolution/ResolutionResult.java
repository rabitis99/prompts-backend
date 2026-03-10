package org.example.sharedprompts.domain.prompt.application.semantic.resolution;

import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;

import java.util.List;

/** 시맨틱 해석(생성 플로우) 결과 타입 */
public final class ResolutionResult {

    private ResolutionResult() {}

    /** axis_sources 조립용 메타데이터 */
    public record ResolutionMetadata(
            boolean fallbackIntentUsed,
            boolean userProvidedIntent,
            boolean userProvidedRole,
            boolean userProvidedAction,
            boolean isExtraction
    ) {
        public static ResolutionMetadata forExtraction() {
            return new ResolutionMetadata(false, false, false, false, true);
        }
    }

    public record Result(boolean success, ConfirmedSemanticAxes axes, List<String> errors, ResolutionMetadata metadata) {
        public static Result ok(ConfirmedSemanticAxes axes, ResolutionMetadata metadata) {
            return new Result(true, axes, List.of(), metadata);
        }

        public static Result fail(List<String> errors) {
            return new Result(false, null, errors != null ? List.copyOf(errors) : List.of(), null);
        }
    }
}
