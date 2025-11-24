package org.example.sharedprompts.domain.prompt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PromptCategory {

    PRODUCTIVITY("생산성", "업무 자동화, 시간 단축 등 생산성 관련 프롬프트"),
    DEVELOPMENT("개발", "프로그래밍, 코드 리뷰, 에러 해결 관련 프롬프트"),
    MARKETING("마케팅", "카피라이팅, 광고 문구, 마케팅 전략 프롬프트"),
    CONTENT("콘텐츠 제작", "블로그, 유튜브, SNS 콘텐츠 제작 관련 프롬프트"),
    STUDY("학습", "언어 공부, 시험 대비 등 학습용 프롬프트"),
    BUSINESS("비즈니스", "기획서, 보고서, 사업 전략 관련 프롬프트"),
    DESIGN("디자인", "UI/UX, 그래픽 디자인, 아이디어 발상"),
    ETC("기타", "기타 모든 카테고리");

    private final String name;
    private final String description;
}