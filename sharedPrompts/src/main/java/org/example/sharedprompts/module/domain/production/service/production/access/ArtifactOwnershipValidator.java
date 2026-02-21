package org.example.sharedprompts.module.domain.production.service.production.access;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactEntity;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactDetailRepository;
import org.example.sharedprompts.module.domain.production.repository.production.ProductionArtifactRepository;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아티팩트/프로덕션 소유권 검증을 단일화합니다.
 * <p>
 * - Controller/Application 레이어에서 중복되던 "userId 일치 여부" 검증을 공통화
 * - 보안 취약점(우회 경로) 방지를 위해 신규 진입점이 생겨도 재사용 가능
 */
@Component
@RequiredArgsConstructor
public class ArtifactOwnershipValidator {

    private final ProductionArtifactRepository productionArtifactRepository;
    private final ProductionArtifactDetailRepository artifactDetailRepository;

    @Transactional(readOnly = true)
    public ProductionArtifactEntity validateProductionOwner(Long productionId, Long userId) {
        ProductionArtifactEntity production = productionArtifactRepository
                .findByIdWithArtifacts(productionId)
                .orElseThrow(() -> new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND));

        return validateProductionOwner(production, userId);
    }

    public ProductionArtifactEntity validateProductionOwner(ProductionArtifactEntity production, Long userId) {
        if (production == null) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND);
        }
        if (production.getUserId() == null || !production.getUserId().equals(userId)) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_FORBIDDEN);
        }
        return production;
    }

    @Transactional(readOnly = true)
    public ProductionArtifactDetailEntity validateArtifactOwner(Long artifactId, Long userId) {
        ProductionArtifactDetailEntity artifact = artifactDetailRepository.findByIdWithArtifact(artifactId)
                .orElseThrow(() -> new BaseException(ModuleErrorCode.ARTIFACT_NOT_FOUND));

        ProductionArtifactEntity production = artifact.getArtifact();
        if (production == null) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_NOT_FOUND);
        }

        if (production.getUserId() == null || !production.getUserId().equals(userId)) {
            throw new BaseException(ModuleErrorCode.PRODUCTION_FORBIDDEN);
        }

        return artifact;
    }

    /**
     * productionId·artifactId로 아티팩트 디테일을 직접 조회하고 소유권 검증 후 반환합니다.
     * 프로덕션 전체 아티팩트를 로드하지 않아 메모리·DB 부하를 줄입니다.
     */
    @Transactional(readOnly = true)
    public ProductionArtifactDetailEntity getArtifactByProductionAndId(Long productionId, Long artifactId, Long userId) {
        ProductionArtifactDetailEntity detail = artifactDetailRepository
                .findByIdAndArtifact_Id(artifactId, productionId)
                .orElseThrow(() -> new BaseException(ModuleErrorCode.ARTIFACT_NOT_FOUND));
        validateProductionOwner(detail.getArtifact(), userId);
        return detail;
    }
}


