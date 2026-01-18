package org.example.sharedprompts.domain.audit.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.audit.AuditLog;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.example.sharedprompts.domain.user.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.example.sharedprompts.domain.audit.QAuditLog.auditLog;
import static org.example.sharedprompts.domain.user.QUser.user;

@RequiredArgsConstructor
public class CustomAuditLogRepositoryImpl implements CustomAuditLogRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<AuditLog> findAdminActivities(Pageable pageable) {
        return searchInternal(null, null, null, null, null, Role.ROLE_ADMIN, pageable);
    }

    @Override
    public Page<AuditLog> findWithFilters(
            Long actorId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        return searchInternal(actorId, entityType, action, startDate, endDate, null, pageable);
    }

    /**
     * 공통 2-step 페이징 + fetchJoin
     * JOIN FETCH와 페이지네이션 조합 시 발생하는 메모리 페이지네이션 문제를 해결하기 위해
     * 1) 먼저 ID만 조회 (페이징 적용)
     * 2) 전체 개수 조회
     * 3) ID 목록으로 fetch join하여 실제 데이터 조회
     */
    private Page<AuditLog> searchInternal(
            Long actorId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Role role,
            Pageable pageable
    ) {
        BooleanExpression where = buildWhereCondition(actorId, entityType, action, startDate, endDate, role);

        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(auditLog.id)
                .from(auditLog)
                .join(auditLog.actor, user)
                .where(where)
                .orderBy(auditLog.createdAt.desc(), auditLog.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(auditLog.count())
                .from(auditLog)
                .join(auditLog.actor, user)
                .where(where)
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (actor는 필수이므로 innerJoin)
        List<AuditLog> content = queryFactory
                .selectFrom(auditLog)
                .innerJoin(auditLog.actor, user).fetchJoin()
                .where(auditLog.id.in(ids))
                .orderBy(auditLog.createdAt.desc(), auditLog.id.desc())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 동적 where 조건 생성
     * QueryDSL의 null 안전 조합 활용
     */
    private BooleanExpression buildWhereCondition(
            Long actorId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Role role
    ) {
        BooleanExpression condition = null;

        if (actorId != null) {
            condition = combine(condition, auditLog.actor.id.eq(actorId));
        }
        if (entityType != null) {
            condition = combine(condition, auditLog.entityType.eq(entityType));
        }
        if (action != null) {
            condition = combine(condition, auditLog.action.eq(action));
        }
        if (startDate != null) {
            condition = combine(condition, auditLog.createdAt.goe(startDate));
        }
        if (endDate != null) {
            condition = combine(condition, auditLog.createdAt.loe(endDate));
        }
        if (role != null) {
            condition = combine(condition, user.role.eq(role));
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
}

