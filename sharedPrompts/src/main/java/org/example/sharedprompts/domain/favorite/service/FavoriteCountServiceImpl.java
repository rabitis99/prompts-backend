package org.example.sharedprompts.domain.favorite.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.shared.service.BaseCountService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FavoriteCountServiceImpl implements FavoriteCountService {

    private final BaseCountService baseCountService;

    private String promptFavoriteKey(Long promptId) {
        return "favorite:prompt:" + promptId;
    }

    @Override
    public void incrementPromptFavoriteCount(Long promptId) {
        baseCountService.increment(promptFavoriteKey(promptId));
    }

    @Override
    public void decrementPromptFavoriteCount(Long promptId) {
        baseCountService.decrement(promptFavoriteKey(promptId));
    }

    @Override
    public Map<Long, Long> getPromptFavoriteCounts(List<Long> promptIds) {
        return baseCountService.getCounts(promptIds, this::promptFavoriteKey);
    }
}

