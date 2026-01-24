package org.example.sharedprompts.global.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 애플리케이션 전역 상수
 * 
 * 공통으로 사용되는 상수 값들을 정의합니다.
 * 모든 필드는 static이므로 인스턴스화를 방지합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constant {
    public static final String STATE_KEY = "state";
    public static final String ACCESS_TOKEN_KEY = "access_token";
    public static final String REFRESH_TOKEN_KEY = "refresh_token";
    public static final String PROVIDER_KEY = "provider";
    public static final String PROVIDER_ID_KEY = "provider_id";

    public static final List<String> ADJECTIVES = List.of(
            "빠른", "멋진", "귀여운", "행복한", "똑똑한",
            "용감한", "신나는", "작은", "큰", "영리한");

    public static final List<String> NOUNS = List.of(
            "호랑이", "토끼", "펭귄", "곰", "여우",
            "고양이", "강아지", "사자", "늑대", "부엉이");
}
