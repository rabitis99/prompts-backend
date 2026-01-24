package org.example.sharedprompts.domain.user;

/**
 * 비밀번호 검증을 위한 도메인 레벨 인터페이스.
 *
 * 인프라(SPRING Security 등) 구현체는 이 인터페이스를 구현하기만 하면 되고,
 * User 엔티티는 구체 구현이나 라이브러리에 의존하지 않습니다.
 */
public interface PasswordVerifier {

    boolean matches(String rawPassword, String encodedPassword);
}


