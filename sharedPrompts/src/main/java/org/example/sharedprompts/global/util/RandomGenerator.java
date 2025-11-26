package org.example.sharedprompts.global.util;

import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

public class RandomGenerator {

    private static final String[] ADJECTIVES = {
            "빠른", "멋진", "귀여운", "행복한", "똑똑한", "용감한", "신나는", "작은", "큰", "영리한"
    };

    private static final String[] NOUNS = {
            "호랑이", "토끼", "펭귄", "곰", "여우", "고양이", "강아지", "사자", "늑대", "부엉이"
    };
    private static final SecureRandom secureRandom = new SecureRandom();

    private static final Random RANDOM = new Random();

    private RandomGenerator() {}

    public static String randomNickname() {
        String adjective = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
        int number = RANDOM.nextInt(9000) + 1000; // 1000~9999
        return adjective + noun + number;
    }

    public static String randomUUID() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);

        // UUID v4 규격 맞춤
        randomBytes[6] &= 0x0f;
        randomBytes[6] |= 0x40;
        randomBytes[8] &= 0x3f;
        randomBytes[8] |= (byte) 0x80;

        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (randomBytes[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (randomBytes[i] & 0xff);
        }

        return new UUID(msb, lsb).toString();
    }

    public static String randomKey() {
        return randomUUID();
    }

    public static String randomState() {
        return randomUUID();
    }
}
