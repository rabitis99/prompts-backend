package org.example.sharedprompts.module.domain.delivery.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.delivery.api.type.DeliveryType;
import org.example.sharedprompts.module.domain.delivery.entity.DeliveryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.example.sharedprompts.module.domain.delivery.entity.QDeliveryEntity.deliveryEntity;

@RequiredArgsConstructor
public class CustomDeliveryRepositoryImpl implements CustomDeliveryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<DeliveryEntity> findPageByUserId(Long userId, Pageable pageable) {
        return findPageInternal(userId, null, pageable);
    }

    @Override
    public Page<DeliveryEntity> findPageByUserIdAndDeliveryType(Long userId, DeliveryType deliveryType, Pageable pageable) {
        return findPageInternal(userId, deliveryType, pageable);
    }

    private Page<DeliveryEntity> findPageInternal(Long userId, DeliveryType deliveryType, Pageable pageable) {
        BooleanExpression where = buildWhereCondition(userId, deliveryType);

        List<Long> ids = queryFactory
                .select(deliveryEntity.id)
                .from(deliveryEntity)
                .where(where)
                .orderBy(deliveryEntity.createdAt.desc(), deliveryEntity.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(deliveryEntity.count())
                .from(deliveryEntity)
                .where(where)
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<DeliveryEntity> content = queryFactory
                .selectFrom(deliveryEntity)
                .where(deliveryEntity.id.in(ids))
                .orderBy(deliveryEntity.createdAt.desc(), deliveryEntity.id.desc())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression buildWhereCondition(Long userId, DeliveryType deliveryType) {
        BooleanExpression condition = deliveryEntity.userId.eq(userId);
        if (deliveryType != null) {
            condition = condition.and(deliveryEntity.deliveryType.eq(deliveryType));
        }
        return condition;
    }
}

