package org.example.sharedprompts.domain.follow.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import org.example.sharedprompts.domain.follow.FollowStatus;

import static org.example.sharedprompts.domain.follow.QFollow.follow;

/**
 * Follow 관련 QueryDSL용 공통 Predicate 모음.
 *
 * - BLOCKED 양방향 조건을 한 곳에서 관리한다.
 */
public final class FollowPredicates {

    private FollowPredicates() {
    }

    /**
     * userA 와 userB 사이에 BLOCKED 관계가 존재하는지 여부를 표현하는 서브쿼리 exists 조건.
     *
     * <pre>
     * EXISTS (
     *   SELECT 1 FROM Follow f
     *   WHERE f.status = BLOCKED
     *     AND (
     *       (f.followerId = :userA AND f.followingId = :userB)
     *       OR (f.followerId = :userB AND f.followingId = :userA)
     *     )
     * )
     * </pre>
     *
     * @param userAId      상수 형태의 사용자 ID (예: viewerId)
     * @param userBIdExpr  경로/식 형태의 사용자 ID (예: prompt.author.id)
     */
    public static BooleanExpression blockedBetween(Long userAId, NumberExpression<Long> userBIdExpr) {
        if (userAId == null || userBIdExpr == null) {
            return null;
        }

        return JPAExpressions
                .selectOne()
                .from(follow)
                .where(
                        follow.status.eq(FollowStatus.BLOCKED)
                                .and(
                                        follow.followerId.eq(userAId)
                                                .and(follow.followingId.eq(userBIdExpr))
                                                .or(
                                                        follow.followerId.eq(userBIdExpr)
                                                                .and(follow.followingId.eq(userAId))
                                                )
                                )
                )
                .exists();
    }

    /**
     * userA 와 userB 사이에 BLOCKED 관계가 존재하지 않는(not exists) 조건.
     *
     * - searchPrompts 등에서 "BLOCKED 가 아닌 프롬프트" 필터링에 사용.
     */
    public static BooleanExpression notBlockedBetween(Long userAId, NumberExpression<Long> userBIdExpr) {
        BooleanExpression blocked = blockedBetween(userAId, userBIdExpr);
        return blocked != null ? blocked.not() : null;
    }
}


