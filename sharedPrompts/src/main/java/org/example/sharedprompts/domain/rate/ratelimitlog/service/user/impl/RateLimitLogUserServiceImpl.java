package org.example.sharedprompts.domain.rate.ratelimitlog.service.user.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.rate.ratelimitlog.service.user.RateLimitLogUserService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Rate Limit 로그용 User 조회 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitLogUserServiceImpl implements RateLimitLogUserService {

    private final UserRepository userRepository;

    @Override
    public User findUserSafely(Long userId) {
        if (userId == null) {
            return null;
        }

        try {
            return userRepository.findById(userId).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to fetch user for rate limit log: userId={}", userId, e);
            return null;
        }
    }
}





