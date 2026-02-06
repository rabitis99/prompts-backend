package org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.cashback;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.entity.Cashback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import static org.example.sharedprompts.domain.payment.domain.entity.QCashback.cashback;
import static org.example.sharedprompts.domain.payment.domain.entity.QPayment.payment;
import static org.example.sharedprompts.domain.user.QUser.user;

@RequiredArgsConstructor
public class CustomCashbackRepositoryImpl implements CustomCashbackRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 2-step 페이징 + fetchJoin
     * 캐시백 내역 조회 시 N+1 문제 해결
     */
    @Override
    public Page<Cashback> findByUserIdWithFetchJoin(Long userId, Pageable pageable) {
        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(cashback.id)
                .from(cashback)
                .where(cashback.user.id.eq(userId))
                .orderBy(cashback.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(cashback.count())
                .from(cashback)
                .where(cashback.user.id.eq(userId))
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (user, payment 포함)
        List<Cashback> fetchedCashbacks = queryFactory
                .selectFrom(cashback)
                .leftJoin(cashback.user, user).fetchJoin()
                .leftJoin(cashback.payment, payment).fetchJoin()
                .where(cashback.id.in(ids))
                .fetch();

        // ids 순서대로 content 정렬 (createdAt DESC 순서 유지)
        Map<Long, Cashback> cashbackMap = fetchedCashbacks.stream()
                .collect(Collectors.toMap(Cashback::getId, c -> c));

        List<Cashback> content = ids.stream()
                .map(cashbackMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 2-step 페이징 + fetchJoin
     * 미지급 캐시백 목록 조회 시 N+1 문제 해결
     */
    @Override
    public Page<Cashback> findUnpaidByUserIdWithFetchJoin(Long userId, Pageable pageable) {
        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(cashback.id)
                .from(cashback)
                .where(
                        cashback.user.id.eq(userId),
                        cashback.paid.eq(false)
                )
                .orderBy(cashback.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(cashback.count())
                .from(cashback)
                .where(
                        cashback.user.id.eq(userId),
                        cashback.paid.eq(false)
                )
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (user, payment 포함)
        List<Cashback> fetchedCashbacks = queryFactory
                .selectFrom(cashback)
                .leftJoin(cashback.user, user).fetchJoin()
                .leftJoin(cashback.payment, payment).fetchJoin()
                .where(cashback.id.in(ids))
                .fetch();

        // ids 순서대로 content 정렬 (createdAt ASC 순서 유지)
        Map<Long, Cashback> cashbackMap = fetchedCashbacks.stream()
                .collect(Collectors.toMap(Cashback::getId, c -> c));

        List<Cashback> content = ids.stream()
                .map(cashbackMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 전체 미지급 캐시백 목록 조회 (관리자용)
     * 2-step 페이징 + fetchJoin
     */
    @Override
    public Page<Cashback> findAllUnpaidWithFetchJoin(Pageable pageable) {
        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(cashback.id)
                .from(cashback)
                .where(cashback.paid.eq(false))
                .orderBy(cashback.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(cashback.count())
                .from(cashback)
                .where(cashback.paid.eq(false))
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (user, payment 포함)
        List<Cashback> fetchedCashbacks = queryFactory
                .selectFrom(cashback)
                .leftJoin(cashback.user, user).fetchJoin()
                .leftJoin(cashback.payment, payment).fetchJoin()
                .where(cashback.id.in(ids))
                .fetch();

        // ids 순서대로 content 정렬 (createdAt ASC 순서 유지)
        Map<Long, Cashback> cashbackMap = fetchedCashbacks.stream()
                .collect(Collectors.toMap(Cashback::getId, c -> c));

        List<Cashback> content = ids.stream()
                .map(cashbackMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }
}
