package org.example.sharedprompts.domain.prompt.common.enums.action.category.content_creation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

/** 추천/리뷰 관련 액션 타입 (content_creation 패키지에 배치). */
@Getter
@AllArgsConstructor
public enum RecommendationActionType implements ActionTypeInterface, StableKeyedEnum {
    BOOK_RECOMMENDATION("ACTION.RECOMMENDATION.BOOK_RECOMMENDATION", "도서 추천", "Book Recommendation", "本の推薦", ActionGroup.RECOMMENDATION),
    MOVIE_RECOMMENDATION("ACTION.RECOMMENDATION.MOVIE_RECOMMENDATION", "영화 추천", "Movie Recommendation", "映画の推薦", ActionGroup.RECOMMENDATION),
    RESTAURANT_RECOMMENDATION("ACTION.RECOMMENDATION.RESTAURANT_RECOMMENDATION", "맛집 추천", "Restaurant Recommendation", "レストラン推薦", ActionGroup.RECOMMENDATION),
    PRODUCT_REVIEW("ACTION.RECOMMENDATION.PRODUCT_REVIEW", "제품 리뷰", "Product Review", "製品レビュー", ActionGroup.RECOMMENDATION);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}
