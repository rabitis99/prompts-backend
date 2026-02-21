package org.example.sharedprompts.module.domain.production.entity.production;

import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.contract.result.ArtifactType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProductionArtifactEntity Aggregate Root 테스트")
class ProductionArtifactEntityTest {

    private ProductionArtifactEntity emptyArtifact() {
        return ProductionArtifactEntity.builder()
                .jobId(1L)
                .tenantId("tenant-1")
                .userId(10L)
                .commandType(ProductionCommandType.TEXT)
                .build();
    }

    private ProductionArtifactDetailEntity textDetail() {
        return ProductionArtifactDetailEntity.builder()
                .artifactType(ArtifactType.TEXT)
                .content("Hello World")
                .fileName("output.txt")
                .contentType("text/plain")
                .build();
    }

    private ProductionArtifactDetailEntity imageDetail() {
        return ProductionArtifactDetailEntity.builder()
                .artifactType(ArtifactType.IMAGE)
                .s3Key("images/test.png")
                .fileName("test.png")
                .contentType("image/png")
                .build();
    }

    // ===== addArtifact() =====

    @Test
    @DisplayName("addArtifact()로 detail 추가 시 컬렉션에 포함되고 양방향 참조가 설정된다")
    void addArtifact_adds_detail_and_sets_bidirectional_reference() {
        ProductionArtifactEntity artifact = emptyArtifact();
        ProductionArtifactDetailEntity detail = textDetail();

        artifact.addArtifact(detail);

        assertThat(artifact.getArtifacts()).hasSize(1);
        assertThat(artifact.getArtifacts()).contains(detail);
        assertThat(detail.getArtifact()).isSameAs(artifact);
    }

    @Test
    @DisplayName("addArtifact()로 여러 detail을 추가할 수 있다")
    void addArtifact_can_add_multiple_details() {
        ProductionArtifactEntity artifact = emptyArtifact();

        artifact.addArtifact(textDetail());
        artifact.addArtifact(imageDetail());

        assertThat(artifact.getArtifacts()).hasSize(2);
    }

    @Test
    @DisplayName("addArtifact()는 중복 방지 없이 단순 추가 — 중복 방지는 서비스 레이어 책임")
    void addArtifact_does_not_deduplicate() {
        ProductionArtifactEntity artifact = emptyArtifact();
        ProductionArtifactDetailEntity detail = textDetail();

        artifact.addArtifact(detail);
        artifact.addArtifact(detail);

        // ArrayList 기반이므로 중복 허용 — 서비스 레이어에서 방지해야 함
        assertThat(artifact.getArtifacts()).hasSize(2);
    }

    // ===== removeArtifact() =====

    @Test
    @DisplayName("removeArtifact()로 detail 제거 시 컬렉션에서 삭제되고 양방향 참조가 해제된다")
    void removeArtifact_removes_detail_and_clears_reference() {
        ProductionArtifactEntity artifact = emptyArtifact();
        ProductionArtifactDetailEntity detail = textDetail();
        artifact.addArtifact(detail);

        artifact.removeArtifact(detail);

        assertThat(artifact.getArtifacts()).isEmpty();
        assertThat(detail.getArtifact()).isNull();
    }

    @Test
    @DisplayName("primary인 detail을 removeArtifact()로 제거하면 primary 플래그가 해제된다")
    void removeArtifact_unmarks_primary_flag_when_primary_detail_removed() {
        ProductionArtifactEntity artifact = emptyArtifact();
        ProductionArtifactDetailEntity detail = textDetail();
        artifact.addArtifact(detail);
        artifact.markAsPrimary(detail);
        assertThat(detail.isPrimary()).as("precondition: detail이 primary여야 함").isTrue();

        artifact.removeArtifact(detail);

        assertThat(detail.isPrimary()).isFalse();
    }

    @Test
    @DisplayName("컬렉션에 없는 detail을 removeArtifact()로 제거해도 오류가 발생하지 않는다")
    void removeArtifact_with_non_member_detail_does_nothing() {
        ProductionArtifactEntity artifact = emptyArtifact();
        ProductionArtifactDetailEntity outsider = textDetail();

        assertThatCode(() -> artifact.removeArtifact(outsider)).doesNotThrowAnyException();
        assertThat(artifact.getArtifacts()).isEmpty();
    }

    // ===== markAsPrimary() =====

    @Test
    @DisplayName("markAsPrimary()는 지정한 detail을 primary로 설정한다")
    void markAsPrimary_sets_detail_as_primary() {
        ProductionArtifactEntity artifact = emptyArtifact();
        ProductionArtifactDetailEntity detail = textDetail();
        artifact.addArtifact(detail);

        artifact.markAsPrimary(detail);

        assertThat(detail.isPrimary()).isTrue();
        assertThat(artifact.hasPrimaryArtifact()).isTrue();
    }

    @Test
    @DisplayName("markAsPrimary()는 기존 primary를 해제하고 새 detail을 primary로 설정한다")
    void markAsPrimary_replaces_existing_primary() {
        ProductionArtifactEntity artifact = emptyArtifact();
        ProductionArtifactDetailEntity detail1 = textDetail();
        ProductionArtifactDetailEntity detail2 = imageDetail();
        artifact.addArtifact(detail1);
        artifact.addArtifact(detail2);
        artifact.markAsPrimary(detail1);

        artifact.markAsPrimary(detail2);

        assertThat(detail1.isPrimary()).isFalse();
        assertThat(detail2.isPrimary()).isTrue();
    }

    @Test
    @DisplayName("컬렉션에 없는 detail을 markAsPrimary()로 지정해도 artifact의 primary 상태에 영향을 주지 않는다")
    void markAsPrimary_with_non_member_detail_does_not_corrupt_state() {
        ProductionArtifactEntity artifact = emptyArtifact();
        ProductionArtifactDetailEntity member = textDetail();
        ProductionArtifactDetailEntity outsider = imageDetail();
        artifact.addArtifact(member);
        artifact.markAsPrimary(member);

        artifact.markAsPrimary(outsider);

        assertThat(member.isPrimary()).isTrue();
        assertThat(outsider.isPrimary()).isFalse();
    }

    @Test
    @DisplayName("markAsPrimary() 후 primary는 항상 정확히 하나만 존재한다")
    void markAsPrimary_ensures_exactly_one_primary() {
        ProductionArtifactEntity artifact = emptyArtifact();
        ProductionArtifactDetailEntity d1 = textDetail();
        ProductionArtifactDetailEntity d2 = imageDetail();
        ProductionArtifactDetailEntity d3 = textDetail();
        artifact.addArtifact(d1);
        artifact.addArtifact(d2);
        artifact.addArtifact(d3);

        artifact.markAsPrimary(d1);
        artifact.markAsPrimary(d2);
        artifact.markAsPrimary(d3);

        long primaryCount = artifact.getArtifacts().stream()
                .filter(ProductionArtifactDetailEntity::isPrimary)
                .count();
        assertThat(primaryCount).isEqualTo(1);
        assertThat(d3.isPrimary()).isTrue();
    }

    // ===== getPrimaryArtifact() =====

    @Test
    @DisplayName("getPrimaryArtifact()는 primary로 설정된 detail을 반환한다")
    void getPrimaryArtifact_returns_primary_detail() {
        ProductionArtifactEntity artifact = emptyArtifact();
        ProductionArtifactDetailEntity detail = textDetail();
        artifact.addArtifact(detail);
        artifact.markAsPrimary(detail);

        assertThat(artifact.getPrimaryArtifact()).isSameAs(detail);
    }

    @Test
    @DisplayName("primary가 없으면 getPrimaryArtifact()는 null을 반환한다")
    void getPrimaryArtifact_returns_null_when_no_primary() {
        ProductionArtifactEntity artifact = emptyArtifact();
        artifact.addArtifact(textDetail());

        assertThat(artifact.getPrimaryArtifact()).isNull();
    }

    @Test
    @DisplayName("detail이 없으면 getPrimaryArtifact()는 null을 반환한다")
    void getPrimaryArtifact_returns_null_when_no_details() {
        ProductionArtifactEntity artifact = emptyArtifact();

        assertThat(artifact.getPrimaryArtifact()).isNull();
    }

    // ===== hasPrimaryArtifact() =====

    @Test
    @DisplayName("primary가 없으면 hasPrimaryArtifact()는 false를 반환한다")
    void hasPrimaryArtifact_returns_false_when_no_primary() {
        ProductionArtifactEntity artifact = emptyArtifact();
        artifact.addArtifact(textDetail());

        assertThat(artifact.hasPrimaryArtifact()).isFalse();
    }

    @Test
    @DisplayName("primary가 있으면 hasPrimaryArtifact()는 true를 반환한다")
    void hasPrimaryArtifact_returns_true_when_primary_exists() {
        ProductionArtifactEntity artifact = emptyArtifact();
        ProductionArtifactDetailEntity detail = textDetail();
        artifact.addArtifact(detail);
        artifact.markAsPrimary(detail);

        assertThat(artifact.hasPrimaryArtifact()).isTrue();
    }

    // ===== 복합 시나리오 =====

    @Test
    @DisplayName("detail 추가 → primary 지정 → 다른 detail 추가 → primary 변경 흐름이 일관성을 유지한다")
    void complex_flow_maintains_consistency() {
        ProductionArtifactEntity artifact = emptyArtifact();
        ProductionArtifactDetailEntity text = textDetail();
        ProductionArtifactDetailEntity image = imageDetail();

        artifact.addArtifact(text);
        artifact.markAsPrimary(text);
        artifact.addArtifact(image);
        artifact.markAsPrimary(image);

        assertThat(artifact.getPrimaryArtifact()).isSameAs(image);
        assertThat(text.isPrimary()).isFalse();
        assertThat(image.isPrimary()).isTrue();
        assertThat(artifact.getArtifacts()).hasSize(2);
    }

    @Test
    @DisplayName("primary detail 제거 후 hasPrimaryArtifact()는 false를 반환한다")
    void after_removing_primary_detail_no_primary_exists() {
        ProductionArtifactEntity artifact = emptyArtifact();
        ProductionArtifactDetailEntity detail = textDetail();
        artifact.addArtifact(detail);
        artifact.markAsPrimary(detail);

        artifact.removeArtifact(detail);

        assertThat(artifact.hasPrimaryArtifact()).isFalse();
        assertThat(artifact.getPrimaryArtifact()).isNull();
    }
}
