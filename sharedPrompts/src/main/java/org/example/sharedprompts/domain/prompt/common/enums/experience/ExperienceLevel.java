package org.example.sharedprompts.domain.prompt.common.enums.experience;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nKey;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nRegistry;
import org.example.sharedprompts.domain.prompt.common.i18n.I18nText;

@Getter
@AllArgsConstructor
public enum ExperienceLevel implements StableKeyedEnum {

    BEGINNER(
            "EXPERIENCE_LEVEL.BEGINNER",
            I18nKey.of("experience.beginner.display_name"),
            I18nKey.of("experience.beginner.guideline")
    ),

    INTERMEDIATE(
            "EXPERIENCE_LEVEL.INTERMEDIATE",
            I18nKey.of("experience.intermediate.display_name"),
            I18nKey.of("experience.intermediate.guideline")
    ),

    /**
     * 고급(세분화용) — 내부적으로는 {@link ExperienceLevelBucket#EXPERT} 버킷에 매핑된다.
     *
     * <p>새 엔진 로직에서는 가급적 {@link #EXPERT}만 사용하고,
     * 이 값은 하위 호환을 위해 유지한다.</p>
     */
    @Deprecated
    ADVANCED(
            "EXPERIENCE_LEVEL.ADVANCED",
            I18nKey.of("experience.advanced.display_name"),
            I18nKey.of("experience.advanced.guideline")
    ),

    EXPERT(
            "EXPERIENCE_LEVEL.EXPERT",
            I18nKey.of("experience.expert.display_name"),
            I18nKey.of("experience.expert.guideline")
    );

    /** Stable serialization-safe identifier */
    private final String key;

    /** 표시용 (UI, 로그 등) — 다국어 키 */
    private final I18nKey displayNameKey;

    /** Prompt Guideline — 다국어 키 */
    private final I18nKey guidelineKey;

    static {
        I18nRegistry registry = I18nRegistry.global();

        // Display names (ko 기준, en/ja는 의미 유지 수준으로 정렬)
        registry.register(I18nKey.of("experience.beginner.display_name"),
                I18nText.of("초급", "Beginner", "初級"));
        registry.register(I18nKey.of("experience.intermediate.display_name"),
                I18nText.of("중급", "Intermediate", "中級"));
        registry.register(I18nKey.of("experience.advanced.display_name"),
                I18nText.of("고급", "Advanced", "上級"));
        registry.register(I18nKey.of("experience.expert.display_name"),
                I18nText.of("전문가", "Expert", "専門家"));

        // Guidelines: 기존 ko/en/ja 텍스트를 그대로 등록
        registry.register(I18nKey.of("experience.beginner.guideline"),
                I18nText.of(
                        "초보자 수준: 전문 용어 최소화, 기초부터 단계별 설명, 충분한 예시 제공",
                        "Beginner level: minimize jargon, step-by-step from basics, provide sufficient examples",
                        "初心者レベル：専門用語最小限、基礎から段階的説明、十分な例を提供"
                ));
        registry.register(I18nKey.of("experience.intermediate.guideline"),
                I18nText.of(
                        "중급 수준: 기본 개념 가정, 중요 세부사항과 실용적 팁 집중",
                        "Intermediate level: assume basic knowledge, focus on important details and practical tips",
                        "中級レベル：基本知識を前提、重要な詳細と実践的ヒントに集中"
                ));
        registry.register(I18nKey.of("experience.advanced.guideline"),
                I18nText.of(
                        "고급 수준: 심화 내용, 최적화 기법, 고급 패턴 포함",
                        "Advanced level: in-depth content, optimization techniques, advanced patterns",
                        "上級レベル：高度な内容、最適化手法、高度なパターンを含む"
                ));
        registry.register(I18nKey.of("experience.expert.guideline"),
                I18nText.of(
                        "전문가 수준: 최신 연구, 엣지 케이스, 고급 트레이드오프, 전문 용어 자유 사용",
                        "Expert level: latest research, edge cases, advanced trade-offs, use professional terminology freely",
                        "専門家レベル：最新動向、エッジケース、高度なトレードオフ、専門用語を自由に使用"
                ));
    }

    @Override
    public String key() {
        return key;
    }

    public String getDisplayName() {
        return I18nRegistry.global().lookup(displayNameKey, LanguageType.KOREAN);
    }

    public String getGuidelineByLang(LanguageType lang) {
        return I18nRegistry.global().lookup(guidelineKey, lang);
    }

    /**
     * 영어 기준 경험 수준 가이드라인을 반환한다.
     * (기존 호출부의 getGuidelineEn()을 대체하기 위한 헬퍼)
     */
    public String getGuidelineEn() {
        return getGuidelineByLang(LanguageType.ENGLISH);
    }

    /**
     * 경험 수준을 3단계 버킷으로 단순화한 값으로 변환한다.
     *
     * <ul>
     *   <li>BEGINNER → BEGINNER</li>
     *   <li>INTERMEDIATE → INTERMEDIATE</li>
     *   <li>ADVANCED, EXPERT → EXPERT</li>
     * </ul>
     */
    public ExperienceLevelBucket toBucket() {
        return switch (this) {
            case BEGINNER -> ExperienceLevelBucket.BEGINNER;
            case INTERMEDIATE -> ExperienceLevelBucket.INTERMEDIATE;
            case ADVANCED, EXPERT -> ExperienceLevelBucket.EXPERT;
        };
    }
}
