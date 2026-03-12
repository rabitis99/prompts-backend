package org.example.sharedprompts.domain.prompt.common.enums.action.category.content;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

/**
 * 추천/리뷰 관련 액션 타입 enum
 *
 * <p>생성자 파라미터 순서 (모든 파라미터는 String 타입):
 * <ol>
 *   <li>stableKey - 안정 키 (직렬화/호환성용)</li>
 *   <li>displayNameKo - 표시 이름 (한국어)</li>
 *   <li>displayNameEn - 표시 이름 (영어)</li>
 *   <li>displayNameJa - 표시 이름 (일본어)</li>
 * </ol>
 * </p>
 */
@Getter
@AllArgsConstructor
public enum RecommendationActionType implements ActionTypeInterface, StableKeyedEnum {
    BOOK_RECOMMENDATION("ACTION.RECOMMENDATION.BOOK_RECOMMENDATION", "도서 추천", "Book Recommendation", "本の推薦", OutputBehaviorType.LONG_FORM_WRITING),
    MOVIE_RECOMMENDATION("ACTION.RECOMMENDATION.MOVIE_RECOMMENDATION", "영화 추천", "Movie Recommendation", "映画の推薦", OutputBehaviorType.LONG_FORM_WRITING),
    RESTAURANT_RECOMMENDATION("ACTION.RECOMMENDATION.RESTAURANT_RECOMMENDATION", "맛집 추천", "Restaurant Recommendation", "レストラン推薦", OutputBehaviorType.LONG_FORM_WRITING),
    PRODUCT_REVIEW("ACTION.RECOMMENDATION.PRODUCT_REVIEW", "제품 리뷰", "Product Review", "製品レビュー", OutputBehaviorType.LONG_FORM_WRITING);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.GENERAL);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

