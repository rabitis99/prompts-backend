package org.example.sharedprompts.domain.payment.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.sharedprompts.domain.payment.QPayment.payment;
import static org.example.sharedprompts.domain.user.QUser.user;

@RequiredArgsConstructor
public class CustomPaymentRepositoryImpl implements CustomPaymentRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 2-step 페이징 + fetchJoin
     * 결제 내역 조회 시 N+1 문제 해결
     */
    @Override
    public Page<Payment> findByUserIdWithFetchJoin(Long userId, Pageable pageable) {
        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(payment.id)
                .from(payment)
                .where(payment.user.id.eq(userId))
                .orderBy(payment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(payment.count())
                .from(payment)
                .where(payment.user.id.eq(userId))
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (user 포함)
        List<Payment> fetchedPayments = queryFactory
                .selectFrom(payment)
                .leftJoin(payment.user, user).fetchJoin()
                .where(payment.id.in(ids))
                .fetch();

        // ids 순서대로 content 정렬 (createdAt DESC 순서 유지)
        Map<Long, Payment> paymentMap = fetchedPayments.stream()
                .collect(Collectors.toMap(Payment::getId, p -> p));

        List<Payment> content = ids.stream()
                .map(paymentMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 전체 결제 내역 조회 (관리자용)
     * 2-step 페이징 + fetchJoin
     */
    @Override
    public Page<Payment> findAllWithFetchJoin(Pageable pageable) {
        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(payment.id)
                .from(payment)
                .orderBy(payment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(payment.count())
                .from(payment)
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (user 포함)
        List<Payment> fetchedPayments = queryFactory
                .selectFrom(payment)
                .leftJoin(payment.user, user).fetchJoin()
                .where(payment.id.in(ids))
                .fetch();

        // ids 순서대로 content 정렬 (createdAt DESC 순서 유지)
        Map<Long, Payment> paymentMap = fetchedPayments.stream()
                .collect(Collectors.toMap(Payment::getId, p -> p));

        List<Payment> content = ids.stream()
                .map(paymentMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }
}
