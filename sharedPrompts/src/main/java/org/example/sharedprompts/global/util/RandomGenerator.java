package org.example.sharedprompts.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.global.constant.Constant;

import java.util.List;
import java.util.Random;
import java.util.UUID;

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

    private static final Random RANDOM = new Random();

    public static String randomNickname() {
        String adjective = ADJECTIVES.get(RANDOM.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(RANDOM.nextInt(NOUNS.size()));
        int number = RANDOM.nextInt(9000) + 1000; // 1000~9999
        return adjective + noun + number;
    }

    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    public static String randomKey() {
        return randomUUID();
    }

    public static String randomState() {
        return randomUUID();
    }
}
