package org.example.sharedprompts.domain.prompt.common.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum AiMlActionType implements ActionTypeInterface {
    MODEL_TRAINING("모델 훈련", "Model Training", "モデル訓練"),
    PREDICTION("예측", "Prediction", "予測"),
    DATA_PREPROCESSING("데이터 전처리", "Data Preprocessing", "データ前処理"),
    MODEL_EVALUATION("모델 평가", "Model Evaluation", "モデル評価"),
    HYPERPARAMETER_TUNING("하이퍼파라미터 조정", "Hyperparameter Tuning", "ハイパーパラメータ調整"),
    FEATURE_ENGINEERING("특성 공학", "Feature Engineering", "特徴量エンジニアリング"),
    MODEL_SELECTION("모델 선택", "Model Selection", "モデル選択"),
    DEEP_LEARNING("딥러닝", "Deep Learning", "深層学習"),
    NEURAL_NETWORK_DESIGN("신경망 설계", "Neural Network Design", "ニューラルネットワーク設計"),
    TRANSFER_LEARNING("전이 학습", "Transfer Learning", "転移学習"),
    REINFORCEMENT_LEARNING("강화 학습", "Reinforcement Learning", "強化学習"),
    NATURAL_LANGUAGE_PROCESSING("자연어 처리", "Natural Language Processing", "自然言語処理"),
    COMPUTER_VISION("컴퓨터 비전", "Computer Vision", "コンピュータビジョン"),
    MODEL_DEPLOYMENT("모델 배포", "Model Deployment", "モデルデプロイ"),
    MODEL_MONITORING("모델 모니터링", "Model Monitoring", "モデル監視"),
    A_B_TESTING("A/B 테스트", "A/B Testing", "A/Bテスト"),
    DATA_LABELING("데이터 라벨링", "Data Labeling", "データラベリング"),
    MODEL_OPTIMIZATION("모델 최적화", "Model Optimization", "モデル最適化"),
    EXPLAINABLE_AI("설명 가능한 AI", "Explainable AI", "説明可能なAI");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.TECHNICAL);
    }
}

