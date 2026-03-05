package org.example.sharedprompts.domain.prompt.common.enums;

/**
 * 사용자 경험 수준을 3단계 버킷으로 단순화한 타입.
 *
 * <p>외부 API 및 UI에서는 {@link ExperienceLevel}의 세부 값을 그대로 사용하되,
 * 엔진 내부 로직(설명 깊이, 전제 지식 수준, 장황함 조절 등)은 이 버킷을 기준으로
 * 동작하도록 설계할 수 있다.</p>
 */
public enum ExperienceLevelBucket {
    BEGINNER,
    INTERMEDIATE,
    EXPERT
}

