package org.example.sharedprompts.domain.prompt.common.enums.action.category.development;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
public enum AiMlActionType implements ActionTypeInterface, StableKeyedEnum {
    MODEL_TRAINING("ACTION.AIML.MODEL_TRAINING", "모델 훈련", "Model Training", "モデル訓練", ActionGroup.MODEL_TRAINING),
    PREDICTION("ACTION.AIML.PREDICTION", "예측", "Prediction", "予測", ActionGroup.MODEL_EVALUATION),
    DATA_PREPROCESSING("ACTION.AIML.DATA_PREPROCESSING", "데이터 전처리", "Data Preprocessing", "データ前処理", ActionGroup.DATA_PREPARATION_FOR_ML),
    MODEL_EVALUATION("ACTION.AIML.MODEL_EVALUATION", "모델 평가", "Model Evaluation", "モデル評価", ActionGroup.MODEL_EVALUATION),
    HYPERPARAMETER_TUNING("ACTION.AIML.HYPERPARAMETER_TUNING", "하이퍼파라미터 조정", "Hyperparameter Tuning", "ハイパーパラメータ調整", ActionGroup.MODEL_TRAINING),
    FEATURE_ENGINEERING("ACTION.AIML.FEATURE_ENGINEERING", "특성 공학", "Feature Engineering", "特徴量エンジニアリング", ActionGroup.DATA_PREPARATION_FOR_ML),
    MODEL_SELECTION("ACTION.AIML.MODEL_SELECTION", "모델 선택", "Model Selection", "モデル選択", ActionGroup.MODEL_TRAINING),
    DEEP_LEARNING("ACTION.AIML.DEEP_LEARNING", "딥러닝", "Deep Learning", "深層学習", ActionGroup.MODEL_TRAINING),
    NEURAL_NETWORK_DESIGN("ACTION.AIML.NEURAL_NETWORK_DESIGN", "신경망 설계", "Neural Network Design", "ニューラルネットワーク設計", ActionGroup.MODEL_TRAINING),
    TRANSFER_LEARNING("ACTION.AIML.TRANSFER_LEARNING", "전이 학습", "Transfer Learning", "転移学習", ActionGroup.MODEL_TRAINING),
    REINFORCEMENT_LEARNING("ACTION.AIML.REINFORCEMENT_LEARNING", "강화 학습", "Reinforcement Learning", "強化学習", ActionGroup.MODEL_TRAINING),
    NATURAL_LANGUAGE_PROCESSING("ACTION.AIML.NATURAL_LANGUAGE_PROCESSING", "자연어 처리", "Natural Language Processing", "自然言語処理", ActionGroup.MODEL_TRAINING),
    COMPUTER_VISION("ACTION.AIML.COMPUTER_VISION", "컴퓨터 비전", "Computer Vision", "コンピュータビジョン", ActionGroup.MODEL_TRAINING),
    MODEL_DEPLOYMENT("ACTION.AIML.MODEL_DEPLOYMENT", "모델 배포", "Model Deployment", "モデルデプロイ", ActionGroup.MODEL_DEPLOYMENT),
    MODEL_MONITORING("ACTION.AIML.MODEL_MONITORING", "모델 모니터링", "Model Monitoring", "モデル監視", ActionGroup.MODEL_DEPLOYMENT),
    A_B_TESTING("ACTION.AIML.A_B_TESTING", "A/B 테스트", "A/B Testing", "A/Bテスト", ActionGroup.MODEL_EVALUATION),
    DATA_LABELING("ACTION.AIML.DATA_LABELING", "데이터 라벨링", "Data Labeling", "データラベリング", ActionGroup.DATA_PREPARATION_FOR_ML),
    MODEL_OPTIMIZATION("ACTION.AIML.MODEL_OPTIMIZATION", "모델 최적화", "Model Optimization", "モデル最適化", ActionGroup.MODEL_EVALUATION),
    EXPLAINABLE_AI("ACTION.AIML.EXPLAINABLE_AI", "설명 가능한 AI", "Explainable AI", "説明可能なAI", ActionGroup.MODEL_EVALUATION);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    AiMlActionType(String stableKey, String displayNameKo, String displayNameEn, String displayNameJa, ActionGroup actionGroup) {
        this.stableKey = stableKey;
        this.displayNameKo = displayNameKo;
        this.displayNameEn = displayNameEn;
        this.displayNameJa = displayNameJa;
        this.actionGroup = actionGroup;
    }

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

