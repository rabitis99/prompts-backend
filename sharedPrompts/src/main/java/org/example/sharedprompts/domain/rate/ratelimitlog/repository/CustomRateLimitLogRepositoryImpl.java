package org.example.sharedprompts.domain.rate.ratelimitlog.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.rate.ratelimitlog.RateLimitLog;
import org.example.sharedprompts.domain.rate.ratelimitlog.enums.RateLimitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.sharedprompts.domain.rate.ratelimitlog.QRateLimitLog.rateLimitLog;
import static org.example.sharedprompts.domain.rate.ratelimitlog.repository.query.RateLimitLogQueries.*;
import static org.example.sharedprompts.domain.user.QUser.user;

/**
 * Rate Limit 로그 커스텀 Repository 구현체
 * 
 * QueryDSL을 사용하여 복잡한 조회 쿼리를 실행합니다.
 * 2-step 페이징과 fetchJoin을 활용하여 N+1 문제를 방지합니다.
 * 통계 쿼리는 집계 함수 특성상 JPQL/네이티브 쿼리를 사용합니다.
 */
@Repository
@RequiredArgsConstructor
public class CustomRateLimitLogRepositoryImpl implements CustomRateLimitLogRepository {

    private final JPAQueryFactory queryFactory;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<RateLimitLog> findByConditions(
            Long userId,
            String clientIp,
            String ruleName,
            RateLimitType rateLimitType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        BooleanExpression where = buildWhereCondition(
                userId, clientIp, ruleName, rateLimitType, startDate, endDate
        );

        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(rateLimitLog.id)
                .from(rateLimitLog)
                .where(where)
                .orderBy(rateLimitLog.createdAt.desc(), rateLimitLog.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(rateLimitLog.count())
                .from(rateLimitLog)
                .where(where)
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (user는 nullable이므로 leftJoin)
        List<RateLimitLog> content = queryFactory
                .selectFrom(rateLimitLog)
                .leftJoin(rateLimitLog.user, user).fetchJoin()
                .where(rateLimitLog.id.in(ids))
                .orderBy(rateLimitLog.createdAt.desc(), rateLimitLog.id.desc())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 동적 where 조건 생성
     * QueryDSL의 null 안전 조합 활용
     */
    private BooleanExpression buildWhereCondition(
            Long userId,
            String clientIp,
            String ruleName,
            RateLimitType rateLimitType,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        BooleanExpression condition = null;

        if (userId != null) {
            condition = combine(condition, rateLimitLog.user.id.eq(userId));
        }
        if (clientIp != null && !clientIp.isEmpty()) {
            condition = combine(condition, rateLimitLog.clientIp.eq(clientIp));
        }
        if (ruleName != null && !ruleName.isEmpty()) {
            condition = combine(condition, rateLimitLog.ruleName.eq(ruleName));
        }
        if (rateLimitType != null) {
            condition = combine(condition, rateLimitLog.rateLimitType.eq(rateLimitType));
        }
        if (startDate != null) {
            condition = combine(condition, rateLimitLog.createdAt.goe(startDate));
        }
        if (endDate != null) {
            condition = combine(condition, rateLimitLog.createdAt.loe(endDate));
        }

        return condition;
    }

    /**
     * null-safe BooleanExpression 조합
     */
    private BooleanExpression combine(BooleanExpression condition, BooleanExpression other) {
        if (condition == null) {
            return other;
        }
        if (other == null) {
            return condition;
        }
        return condition.and(other);
    }

    @Override
    public int deleteOldLogs(LocalDateTime cutoffDate) {
        Query query = entityManager.createQuery(DELETE_OLD_LOGS);
        query.setParameter("cutoffDate", cutoffDate);
        return query.executeUpdate();
    }

    @Override
    public Map<Integer, Long> countByHour(LocalDateTime startDate, LocalDateTime endDate) {
        // EXTRACT 함수는 JPA 표준이 아니므로 네이티브 쿼리 사용
        Query query = entityManager.createNativeQuery(COUNT_BY_HOUR);
        setDateParameters(query, startDate, endDate);
        
        List<Object[]> results = getResultList(query);
        
        return results.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> ((Number) row[1]).longValue()
                ));
    }

    @Override
    public Map<String, Long> countByRuleName(LocalDateTime startDate, LocalDateTime endDate) {
        Query query = entityManager.createQuery(COUNT_BY_RULE_NAME);
        setDateParameters(query, startDate, endDate);
        
        List<Object[]> results = getResultList(query);
        
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Override
    public Map<RateLimitType, Long> countByType(LocalDateTime startDate, LocalDateTime endDate) {
        Query query = entityManager.createQuery(COUNT_BY_TYPE);
        setDateParameters(query, startDate, endDate);
        
        List<Object[]> results = getResultList(query);
        
        Map<RateLimitType, Long> result = initializeTypeMap();
        results.forEach(row -> {
            result.put((RateLimitType) row[0], (Long) row[1]);
        });
        
        return result;
    }

    @Override
    public List<Object[]> findTopViolatingIps(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        Query query = entityManager.createQuery(FIND_TOP_VIOLATING_IPS);
        setDateParameters(query, startDate, endDate);
        query.setMaxResults(limit);
        
        return getResultList(query);
    }

    @Override
    public List<Object[]> findTopViolatingUsers(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        Query query = entityManager.createQuery(FIND_TOP_VIOLATING_USERS);
        setDateParameters(query, startDate, endDate);
        query.setMaxResults(limit);
        
        return getResultList(query);
    }

    /**
     * 쿼리에 날짜 파라미터를 설정합니다.
     */
    private void setDateParameters(Query query, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }
    }

    /**
     * 쿼리 결과를 타입 안전하게 Object[] 리스트로 변환합니다.
     */
    @SuppressWarnings("unchecked")
    private List<Object[]> getResultList(Query query) {
        return (List<Object[]>) query.getResultList();
    }

    /**
     * RateLimitType 맵을 초기화합니다.
     * 모든 타입을 0으로 초기화하여 누락된 타입도 포함시킵니다.
     */
    private Map<RateLimitType, Long> initializeTypeMap() {
        Map<RateLimitType, Long> result = new HashMap<>();
        for (RateLimitType type : RateLimitType.values()) {
            result.put(type, 0L);
        }
        return result;
    }
}

