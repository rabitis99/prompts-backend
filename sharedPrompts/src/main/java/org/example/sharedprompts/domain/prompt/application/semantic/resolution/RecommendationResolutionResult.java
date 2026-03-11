package org.example.sharedprompts.domain.prompt.application.semantic.resolution;

import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;

import java.util.List;

/** 시맨틱 해석(추천 플로우) 결과 타입. 생성 플로우의 {@link ResolutionResult}와 동일한 Result 패턴 사용 */
public final class RecommendationResolutionResult {

    private RecommendationResolutionResult() {}

    public record Result(boolean success, RecommendPromptResult result, List<String> errors) {
        public static Result ok(RecommendPromptResult result) {
            return new Result(true, result, List.of());
        }

        public static Result fail(List<String> errors) {
            return new Result(false, null, errors != null ? List.copyOf(errors) : List.of());
        }
    }
}
