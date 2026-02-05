package org.example.sharedprompts.domain.payment.repository.point;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.sharedprompts.domain.payment.QPoint.point;
import static org.example.sharedprompts.domain.payment.QPayment.payment;
import static org.example.sharedprompts.domain.user.QUser.user;

@RequiredArgsConstructor
public class CustomPointRepositoryImpl implements CustomPointRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 2-step 페이징 + fetchJoin
     * 포인트 내역 조회 시 N+1 문제 해결
     * payment는 nullable이므로 leftJoin 사용
     */
    @Override
    public Page<Point> findByUserIdWithFetchJoin(Long userId, Pageable pageable) {
        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(point.id)
                .from(point)
                .where(point.user.id.eq(userId))
                .orderBy(point.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(point.count())
                .from(point)
                .where(point.user.id.eq(userId))
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (user, payment 포함)
        List<Point> fetchedPoints = queryFactory
                .selectFrom(point)
                .leftJoin(point.user, user).fetchJoin()
                .leftJoin(point.payment, payment).fetchJoin()
                .where(point.id.in(ids))
                .fetch();

        // ids 순서대로 content 정렬 (createdAt DESC 순서 유지)
        Map<Long, Point> pointMap = fetchedPoints.stream()
                .collect(Collectors.toMap(Point::getId, p -> p));

        List<Point> content = ids.stream()
                .map(pointMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 2-step 페이징 + fetchJoin
     * 결제와 연관된 포인트 조회 시 N+1 문제 해결
     */
    @Override
    public Page<Point> findByPaymentIdAndUserIdWithFetchJoin(Long paymentId, Long userId, Pageable pageable) {
        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(point.id)
                .from(point)
                .where(
                        point.payment.id.eq(paymentId),
                        point.user.id.eq(userId)
                )
                .orderBy(point.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(point.count())
                .from(point)
                .where(
                        point.payment.id.eq(paymentId),
                        point.user.id.eq(userId)
                )
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (user, payment 포함)
        List<Point> fetchedPoints = queryFactory
                .selectFrom(point)
                .leftJoin(point.user, user).fetchJoin()
                .leftJoin(point.payment, payment).fetchJoin()
                .where(point.id.in(ids))
                .fetch();

        // ids 순서대로 content 정렬 (createdAt DESC 순서 유지)
        Map<Long, Point> pointMap = fetchedPoints.stream()
                .collect(Collectors.toMap(Point::getId, p -> p));

        List<Point> content = ids.stream()
                .map(pointMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 결제와 연관된 포인트 조회 (관리자용 - userId 필터 없음)
     * 2-step 페이징 + fetchJoin
     */
    @Override
    public Page<Point> findByPaymentIdWithFetchJoin(Long paymentId, Pageable pageable) {
        // 1) 페이징 가능한 id 조회
        List<Long> ids = queryFactory
                .select(point.id)
                .from(point)
                .where(point.payment.id.eq(paymentId))
                .orderBy(point.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2) 전체 개수 조회
        Long totalCount = queryFactory
                .select(point.count())
                .from(point)
                .where(point.payment.id.eq(paymentId))
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // 3) fetch join으로 데이터 조회 (user, payment 포함)
        List<Point> fetchedPoints = queryFactory
                .selectFrom(point)
                .leftJoin(point.user, user).fetchJoin()
                .leftJoin(point.payment, payment).fetchJoin()
                .where(point.id.in(ids))
                .fetch();

        // ids 순서대로 content 정렬 (createdAt DESC 순서 유지)
        Map<Long, Point> pointMap = fetchedPoints.stream()
                .collect(Collectors.toMap(Point::getId, p -> p));

        List<Point> content = ids.stream()
                .map(pointMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }
}
