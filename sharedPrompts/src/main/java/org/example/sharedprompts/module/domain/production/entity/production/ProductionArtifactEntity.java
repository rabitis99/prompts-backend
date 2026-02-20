package org.example.sharedprompts.module.domain.production.entity.production;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;

import java.util.ArrayList;
import java.util.List;

/**
 * Production Artifact Aggregate Root
 * 
 * 책임:
 * - AI 결과의 논리적 단위 (Logical Result)
 * - Job 성공 시 생성됨
 * - 하위 엔티티(ProductionArtifactDetailEntity)의 생명주기 완전 통제
 * - primary 지정 책임 통제 (반드시 하나만 존재)
 * 
 * 원칙:
 * - Artifact는 "결과 의미 단위"이지 프로세스가 아니다
 * - 상태는 Job이 관리 (Artifact는 상태를 가지지 않음)
 * - createdAt 정도만 보유 (startedAt, completedAt 제거)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "production_artifacts",
        indexes = {
                @Index(name = "idx_pa_user_id_created_at", columnList = "user_id,created_at"),
                @Index(name = "idx_pa_user_id_command_type", columnList = "user_id,command_type,created_at"),
                @Index(name = "idx_pa_tenant_user", columnList = "tenant_id,user_id,created_at"),
                @Index(name = "idx_pa_job_id", columnList = "job_id")
        }
)
public class ProductionArtifactEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Job ID (FK)
     * Job과의 연관관계는 FK로만 연결 (양방향 불필요)
     */
    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 50)
    private ProductionCommandType commandType;

    @OneToMany(
            mappedBy = "artifact",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ProductionArtifactDetailEntity> artifacts = new ArrayList<>();

    /* =========================
       Aggregate Root Methods
       ========================= */

    /**
     * Artifact Detail 추가
     * 양방향 정합성 보장
     * 
     * 검증은 서비스 레이어에서 수행해야 합니다.
     * 이 메서드는 단순히 상태 변경만 담당합니다.
     */
    public void addArtifact(ProductionArtifactDetailEntity detail) {
        artifacts.add(detail);
        detail.setArtifact(this);
    }

    /**
     * Artifact Detail 제거
     * 양방향 정합성 보장
     * primary로 지정된 detail을 제거하는 경우 primary 상태를 먼저 해제합니다.
     * 
     * 검증은 서비스 레이어에서 수행해야 합니다.
     * 이 메서드는 단순히 상태 변경만 담당합니다.
     */
    public void removeArtifact(ProductionArtifactDetailEntity detail) {
        // 제거 성공 여부를 먼저 확인한 후 primary를 해제해야 함
        // 제거 실패 시 primary 플래그가 조기 해제되는 것을 방지
        if (artifacts.remove(detail)) {
            if (detail.isPrimary()) {
                detail.unmarkPrimary();
            }
            detail.setArtifact(null);
        }
    }

    /**
     * Primary Artifact 지정
     * Aggregate Root에서 통제하여 반드시 하나만 존재하도록 보장
     * 
     * 검증은 서비스 레이어에서 수행해야 합니다.
     * 이 메서드는 단순히 상태 변경만 담당합니다.
     */
    public void markAsPrimary(ProductionArtifactDetailEntity detail) {
        // 기존 primary 제거
        artifacts.stream()
                .filter(ProductionArtifactDetailEntity::isPrimary)
                .forEach(ProductionArtifactDetailEntity::unmarkPrimary);
        
        // 새로운 primary 설정
        // detached 엔티티 전달 시 컬렉션 내의 managed 엔티티를 찾아서 설정해야 함
        // ID가 일치하는 경우 컬렉션 내의 managed 엔티티에 markPrimary()를 호출
        ProductionArtifactDetailEntity managedDetail = detail.getId() != null
                ? artifacts.stream()
                        .filter(a -> a.getId() != null && a.getId().equals(detail.getId()))
                        .findFirst()
                        .orElse(detail)
                : artifacts.contains(detail)
                        ? artifacts.stream()
                                .filter(a -> a == detail)
                                .findFirst()
                                .orElse(detail)
                        : detail;
        
        managedDetail.markPrimary();
    }

    /**
     * Primary Artifact 조회
     */
    public ProductionArtifactDetailEntity getPrimaryArtifact() {
        return artifacts.stream()
                .filter(ProductionArtifactDetailEntity::isPrimary)
                .findFirst()
                .orElse(null);
    }

    /**
     * Primary Artifact 존재 여부 확인
     */
    public boolean hasPrimaryArtifact() {
        return artifacts.stream()
                .anyMatch(ProductionArtifactDetailEntity::isPrimary);
    }
}
