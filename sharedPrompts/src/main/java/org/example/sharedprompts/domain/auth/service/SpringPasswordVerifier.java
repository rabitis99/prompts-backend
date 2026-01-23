package org.example.sharedprompts.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.user.PasswordVerifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Spring Security의 PasswordEncoder를 감싸는 어댑터.
 * 도메인 레벨에서는 PasswordVerifier 인터페이스만 바라봅니다.
 */
@Component
@RequiredArgsConstructor
public class SpringPasswordVerifier implements PasswordVerifier {

    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}


