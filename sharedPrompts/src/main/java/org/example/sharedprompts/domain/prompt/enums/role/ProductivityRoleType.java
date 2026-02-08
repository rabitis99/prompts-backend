package org.example.sharedprompts.domain.prompt.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductivityRoleType implements RoleTypeInterface {
    PRODUCTIVITY_EXPERT(
            "생산성 전문가",
            "효율성을 극대화하고 작업 프로세스를 최적화하는 전문가",
            "Productivity Expert",
            "A specialist who maximizes efficiency and optimizes work processes",
            "生産性専門家",
            "効率性を最大化し、作業プロセスを最適化する専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;
}

