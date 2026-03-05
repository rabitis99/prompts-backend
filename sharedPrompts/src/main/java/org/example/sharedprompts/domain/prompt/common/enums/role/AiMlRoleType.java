package org.example.sharedprompts.domain.prompt.common.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum;

@Getter
@AllArgsConstructor
public enum AiMlRoleType implements RoleTypeInterface, StableKeyedEnum {
    ML_ENGINEER(
            "머신러닝 엔지니어",
            "머신러닝 모델 개발 및 배포 전문가",
            "ML Engineer",
            "A specialist in machine learning model development and deployment",
            "機械学習エンジニア",
            "機械学習モデル開発とデプロイの専門家"
    ),
    DATA_SCIENTIST(
            "데이터 사이언티스트",
            "데이터 분석 및 머신러닝 모델 구축 전문가",
            "Data Scientist",
            "A specialist in data analysis and machine learning model construction",
            "データサイエンティスト",
            "データ分析と機械学習モデル構築の専門家"
    ),
    AI_RESEARCHER(
            "AI 연구원",
            "인공지능 알고리즘 연구 및 개발 전문가",
            "AI Researcher",
            "A specialist in artificial intelligence algorithm research and development",
            "AI研究者",
            "人工知能アルゴリズム研究と開発の専門家"
    ),
    NLP_SPECIALIST(
            "자연어 처리 전문가",
            "자연어 처리 모델 개발 및 최적화 전문가",
            "NLP Specialist",
            "A specialist in natural language processing model development and optimization",
            "自然言語処理専門家",
            "自然言語処理モデル開発と最適化の専門家"
    ),
    COMPUTER_VISION_ENGINEER(
            "컴퓨터 비전 엔지니어",
            "이미지 및 비디오 분석 모델 개발 전문가",
            "Computer Vision Engineer",
            "A specialist in image and video analysis model development",
            "コンピュータビジョンエンジニア",
            "画像と動画分析モデル開発の専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String key() {
        return "ROLE.AIML." + name();
    }
}

