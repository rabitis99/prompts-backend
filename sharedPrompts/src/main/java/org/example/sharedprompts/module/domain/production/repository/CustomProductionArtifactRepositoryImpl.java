package org.example.sharedprompts.module.domain.production.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.api.artifact.ArtifactType;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.entity.ProductionArtifactEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.example.sharedprompts.module.domain.production.entity.QProductionArtifactEntity.productionArtifactEntity;

@RequiredArgsConstructor
public class CustomProductionArtifactRepositoryImpl implements CustomProductionArtifactRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ProductionArtifactEntity> findPageByUserId(Long userId, Pageable pageable) {
        return findPageInternal(userId, null, null, pageable);
    }

    @Override
    public Page<ProductionArtifactEntity> findPageByUserIdAndCommandType(Long userId, ProductionCommandType commandType, Pageable pageable) {
        return findPageInternal(userId, commandType, null, pageable);
    }

    @Override
    public Page<ProductionArtifactEntity> findPageByUserIdAndArtifactType(Long userId, ArtifactType artifactType, Pageable pageable) {
        return findPageInternal(userId, null, artifactType, pageable);
    }

    @Override
    public Long findLatestIdByUserIdAndCommandType(Long userId, ProductionCommandType commandType) {
        return queryFactory
                .select(productionArtifactEntity.id)
                .from(productionArtifactEntity)
                .where(productionArtifactEntity.userId.eq(userId)
                        .and(productionArtifactEntity.commandType.eq(commandType)))
                .orderBy(productionArtifactEntity.createdAt.desc(), productionArtifactEntity.id.desc())
                .limit(1)
                .fetchOne();
    }

    private Page<ProductionArtifactEntity> findPageInternal(
            Long userId,
            ProductionCommandType commandType,
            ArtifactType artifactType,
            Pageable pageable
    ) {
        BooleanExpression where = buildWhereCondition(userId, commandType, artifactType);

        List<Long> ids = queryFactory
                .select(productionArtifactEntity.id)
                .from(productionArtifactEntity)
                .where(where)
                .orderBy(productionArtifactEntity.createdAt.desc(), productionArtifactEntity.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(productionArtifactEntity.count())
                .from(productionArtifactEntity)
                .where(where)
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<ProductionArtifactEntity> content = queryFactory
                .selectFrom(productionArtifactEntity)
                .where(productionArtifactEntity.id.in(ids))
                .orderBy(productionArtifactEntity.createdAt.desc(), productionArtifactEntity.id.desc())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression buildWhereCondition(Long userId, ProductionCommandType commandType, ArtifactType artifactType) {
        BooleanExpression condition = productionArtifactEntity.userId.eq(userId);
        if (commandType != null) {
            condition = condition.and(productionArtifactEntity.commandType.eq(commandType));
        }
        if (artifactType != null) {
            condition = condition.and(productionArtifactEntity.artifactType.eq(artifactType));
        }
        return condition;
    }
}

