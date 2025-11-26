package org.example.sharedprompts.global.constant;

public final class Constant {

    private Constant() {
    }
    public static final String STATE_KEY = "state";
    public static final String ACCESS_TOKEN_KEY = "access_token";
    public static final String REFRESH_TOKEN_KEY = "refresh_token";
    public static final String PROVIDER_KEY = "provider";
    public static final String PROVIDER_ID_KEY = "providerId";
    public static final String[] ADJECTIVES = {
            "빠른", "멋진", "귀여운", "행복한", "똑똑한",
            "용감한", "신나는", "작은", "큰", "영리한"
    };

    public static final String[] NOUNS = {
            "호랑이", "토끼", "펭귄", "곰", "여우",
            "고양이", "강아지", "사자", "늑대", "부엉이"
    };
}
