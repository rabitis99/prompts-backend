package org.example.sharedprompts.module.domain.production.repository.production;

import org.example.sharedprompts.module.domain.production.entity.production.ProductionArtifactDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionArtifactDetailRepository extends JpaRepository<ProductionArtifactDetailEntity, Long> {
}

