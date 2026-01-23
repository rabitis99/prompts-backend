package org.example.sharedprompts.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.global.constant.Constant;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 랜덤 값 생성 유틸리티
 * 
 * 닉네임, UUID, 키 등 다양한 랜덤 값을 생성합니다.
 * 모든 메서드는 static이므로 인스턴스화를 방지합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RandomGenerator {

    private static final List<String> ADJECTIVES = Constant.ADJECTIVES;
    private static final List<String> NOUNS = Constant.NOUNS;
    
    // 닉네임 생성 시 사용할 숫자 범위 상수
    private static final int NICKNAME_NUMBER_MIN = 1000;
    private static final int NICKNAME_NUMBER_MAX = 9999;
    private static final int NICKNAME_NUMBER_RANGE = NICKNAME_NUMBER_MAX - NICKNAME_NUMBER_MIN + 1;

    public static String randomNickname() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));
        int number = random.nextInt(NICKNAME_NUMBER_RANGE) + NICKNAME_NUMBER_MIN;
        return adjective + noun + number;
    }

    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 랜덤 키 생성 (UUID 기반)
     * 
     * @return UUID 문자열
     */
    public static String randomKey() {
        return randomUUID();
    }
}
