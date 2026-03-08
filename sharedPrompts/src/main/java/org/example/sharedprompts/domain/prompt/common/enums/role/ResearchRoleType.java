package org.example.sharedprompts.domain.prompt.common.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResearchRoleType implements RoleTypeInterface {
    RESEARCHER(
            "연구자",
            "과학적 연구 방법론 및 실험 설계 전문가",
            "Researcher",
            "A specialist in scientific research methodology and experiment design",
            "研究者",
            "科学的研究方法論と実験設計の専門家"
    ),
    ACADEMIC_WRITER(
            "학술 논문 작성자",
            "논문 작성 및 학술 연구 방법론 전문가",
            "Academic Writer",
            "A specialist in paper writing and academic research methodology",
            "学術論文執筆者",
            "論文執筆と学術研究方法論の専門家"
    ),
    RESEARCH_METHODOLOGIST(
            "연구 방법론 전문가",
            "연구 설계 및 데이터 해석 방법론 개발 전문가",
            "Research Methodologist",
            "A specialist in research design and data interpretation methodology development",
            "研究方法論専門家",
            "研究設計とデータ解釈方法論開発の専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String key() {
        return "ROLE.RESEARCH." + name();
    }
}

