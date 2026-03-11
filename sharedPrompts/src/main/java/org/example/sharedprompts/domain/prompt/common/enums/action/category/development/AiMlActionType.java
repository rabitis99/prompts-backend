package org.example.sharedprompts.domain.prompt.common.enums.action.category.development;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum AiMlActionType implements ActionTypeInterface, StableKeyedEnum {
    MODEL_TRAINING("모델 훈련", "Model Training", "モデル訓練", OutputBehaviorType.CODE_IMPLEMENTATION),
    PREDICTION("예측", "Prediction", "予測", OutputBehaviorType.CODE_IMPLEMENTATION),
    DATA_PREPROCESSING("데이터 전처리", "Data Preprocessing", "データ前処理", OutputBehaviorType.CODE_IMPLEMENTATION),
    MODEL_EVALUATION("모델 평가", "Model Evaluation", "モデル評価", OutputBehaviorType.CODE_IMPLEMENTATION),
    HYPERPARAMETER_TUNING("하이퍼파라미터 조정", "Hyperparameter Tuning", "ハイパーパラメータ調整", OutputBehaviorType.CODE_IMPLEMENTATION),
    FEATURE_ENGINEERING("특성 공학", "Feature Engineering", "特徴量エンジニアリング", OutputBehaviorType.CODE_IMPLEMENTATION),
    MODEL_SELECTION("모델 선택", "Model Selection", "モデル選択", OutputBehaviorType.CODE_IMPLEMENTATION),
    DEEP_LEARNING("딥러닝", "Deep Learning", "深層学習", OutputBehaviorType.CODE_IMPLEMENTATION),
    NEURAL_NETWORK_DESIGN("신경망 설계", "Neural Network Design", "ニューラルネットワーク設計", OutputBehaviorType.CODE_IMPLEMENTATION),
    TRANSFER_LEARNING("전이 학습", "Transfer Learning", "転移学習", OutputBehaviorType.CODE_IMPLEMENTATION),
    REINFORCEMENT_LEARNING("강화 학습", "Reinforcement Learning", "強化学習", OutputBehaviorType.CODE_IMPLEMENTATION),
    NATURAL_LANGUAGE_PROCESSING("자연어 처리", "Natural Language Processing", "自然言語処理", OutputBehaviorType.CODE_IMPLEMENTATION),
    COMPUTER_VISION("컴퓨터 비전", "Computer Vision", "コンピュータビジョン", OutputBehaviorType.CODE_IMPLEMENTATION),
    MODEL_DEPLOYMENT("모델 배포", "Model Deployment", "モデルデプロイ", OutputBehaviorType.CODE_IMPLEMENTATION),
    MODEL_MONITORING("모델 모니터링", "Model Monitoring", "モデル監視", OutputBehaviorType.CODE_IMPLEMENTATION),
    A_B_TESTING("A/B 테스트", "A/B Testing", "A/Bテスト", OutputBehaviorType.CODE_IMPLEMENTATION),
    DATA_LABELING("데이터 라벨링", "Data Labeling", "データラベリング", OutputBehaviorType.CODE_IMPLEMENTATION),
    MODEL_OPTIMIZATION("모델 최적화", "Model Optimization", "モデル最適化", OutputBehaviorType.CODE_IMPLEMENTATION),
    EXPLAINABLE_AI("설명 가능한 AI", "Explainable AI", "説明可能なAI", OutputBehaviorType.CODE_IMPLEMENTATION);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.AIML." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.TECHNICAL);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

