package org.example.sharedprompts.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 체크섬 생성 유틸리티 클래스
 * 
 * 파일 무결성 검증, 데이터 비교 등을 위한 체크섬 생성 기능을 제공합니다.
 * 모든 메서드는 static이므로 인스턴스화를 방지합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChecksumUtils {

    /**
     * 문자열의 SHA-256 체크섬을 생성합니다.
     * 
     * @param content 체크섬을 생성할 문자열
     * @return SHA-256 해시값 (16진수 문자열)
     * @throws RuntimeException SHA-256 알고리즘을 사용할 수 없는 경우
     */
    public static String generateSha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate SHA-256 checksum: " + e.getMessage(), e);
        }
    }

    /**
     * 바이트 배열의 SHA-256 체크섬을 생성합니다.
     * 
     * @param data 체크섬을 생성할 바이트 배열
     * @return SHA-256 해시값 (16진수 문자열)
     * @throws RuntimeException SHA-256 알고리즘을 사용할 수 없는 경우
     */
    public static String generateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate SHA-256 checksum: " + e.getMessage(), e);
        }
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환합니다.
     * 
     * @param bytes 변환할 바이트 배열
     * @return 16진수 문자열
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

