package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResolvedDomain 단위 테스트")
class ResolvedDomainTest {

    @Test
    @DisplayName("2-arg 생성자로 생성 시 source가 FALLBACK 또는 DEFAULT로 설정됨")
    void twoArgConstructorSetsSource() {
        ResolvedDomain fallback = new ResolvedDomain(TaskDomain.GENERAL, true);
        assertThat(fallback.domain()).isEqualTo(TaskDomain.GENERAL);
        assertThat(fallback.fallback()).isTrue();
        assertThat(fallback.source()).isEqualTo(ResolutionSource.FALLBACK);

        ResolvedDomain resolved = new ResolvedDomain(TaskDomain.TECHNICAL, false);
        assertThat(resolved.fallback()).isFalse();
        assertThat(resolved.source()).isEqualTo(ResolutionSource.DEFAULT);
    }

    @Test
    @DisplayName("3-arg 생성자로 source 명시 가능")
    void threeArgConstructorPreservesSource() {
        ResolvedDomain r = new ResolvedDomain(TaskDomain.CREATIVE, false, ResolutionSource.ACTION_TYPE);
        assertThat(r.source()).isEqualTo(ResolutionSource.ACTION_TYPE);
        assertThat(r.domain()).isEqualTo(TaskDomain.CREATIVE);
    }
}
