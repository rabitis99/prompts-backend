package org.example.sharedprompts.domain.favorite.service;

import java.util.List;
import java.util.Map;

public interface FavoriteCountService {

    void incrementPromptFavoriteCount(Long promptId);

    void decrementPromptFavoriteCount(Long promptId);

    Map<Long, Long> getPromptFavoriteCounts(List<Long> promptIds);
}

