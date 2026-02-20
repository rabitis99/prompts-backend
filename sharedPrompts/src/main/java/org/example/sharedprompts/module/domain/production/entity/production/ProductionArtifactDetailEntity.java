package org.example.sharedprompts.module.domain.production.entity.production;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Production Artifact Detail Entity
 * 
 * 책임:
 * - 실제 저장 단위 (Physical Result)
 * - S3 저장 정보 및 파일/이미지/텍스트 물리적 표현
 * - Primary 플래그 관리
 * 
 * 불변 조건:
 * - TEXT 타입: content 필수, s3Key는 null
 * - FILE/IMAGE 타입: s3Key 필수, content는 null
 * - 무결성은 생성 시점 factory에서만 보장 (JPA lifecycle hook 제거)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "production_artifact_details")
public class ProductionArtifactDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_id", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private ProductionArtifactEntity artifact;

    @Column(name = "is_primary", nullable = false)
    @Getter(AccessLevel.NONE)
    @Builder.Default
    private boolean primary = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false, length = 50)
    private ArtifactType artifactType;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "s3_key", length = 512)
    private String s3Key;

    /**
     * 실제 이미지 파일 경로 (HTML 파일인 경우 내부 이미지 경로)
     * HTML 파일이 아닌 경우 null이며, s3Key와 동일한 값을 가집니다.
     * 엔티티 생성 시점에 미리 추출하여 저장하여 DTO 매핑 시 S3 I/O를 방지합니다.
     */
    @Column(name = "actual_image_path", length = 512)
    private String actualImagePath;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /* =========================
       Domain Behavior
       ========================= */

    /**
     * Primary 플래그 설정
     * Aggregate Root에서 통제되어야 하므로 package-private
     */
    void markPrimary() {
        this.primary = true;
    }

    /**
     * Primary 플래그 해제
     * Aggregate Root에서 통제되어야 하므로 package-private
     */
    void unmarkPrimary() {
        this.primary = false;
    }

    /**
     * Primary 여부 확인
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * 메타데이터 업데이트
     */
    public void updateMetadata(String metadata) {
        this.metadata = metadata;
    }
}
