package org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.domain.entity.UserTierHistory;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.userTier.UserTierHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserTierHistoryJpaAdapter {

    private final UserTierHistoryRepository userTierHistoryRepository;

    public UserTierHistory save(UserTierHistory userTierHistory) {
        return userTierHistoryRepository.save(userTierHistory);
    }

    public Optional<UserTierHistory> findById(Long id) {
        return userTierHistoryRepository.findById(id);
    }

    public Page<UserTierHistory> findByUserIdWithFetchJoin(Long userId, Pageable pageable) {
        return userTierHistoryRepository.findByUserIdWithFetchJoin(userId, pageable);
    }
}




