package org.example.sharedprompts.global.util;

import org.example.sharedprompts.global.constant.Constant;

import java.util.Random;
import java.util.UUID;

public class RandomGenerator {

    private static final String[] ADJECTIVES = Constant.ADJECTIVES;

    private static final String[] NOUNS = Constant.NOUNS;

    private static final Random RANDOM = new Random();

    private RandomGenerator() {}

    public static String randomNickname() {
        String adjective = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
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
