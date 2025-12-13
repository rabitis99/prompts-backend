package org.example.sharedprompts.domain.like.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.Lua.LuaScripts;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


@Service
@RequiredArgsConstructor
public class LikeCountServiceImpl implements LikeCountService {

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<Long> safeDecrScript;

    @PostConstruct
    public void init() {
        safeDecrScript = new DefaultRedisScript<>();
        safeDecrScript.setScriptText(LuaScripts.SAFE_DECREMENT);
        safeDecrScript.setResultType(Long.class);
    }

    private String commentLikeKey(Long commentId) {
        return "like:comment:" + commentId;
    }

    private String promptLikeKey(Long promptId) {
        return "like:prompt:" + promptId;
    }

    @Override
    public void incrementCommentLikeCount(Long commentId) {
        redisTemplate.opsForValue().increment(commentLikeKey(commentId));
    }

    @Override
    public void incrementPromptLikeCount(Long promptId) {
        redisTemplate.opsForValue().increment(promptLikeKey(promptId));
    }

    @Override
    public void decrementCommentLikeCount(Long commentId) {
        safeDecrement(commentLikeKey(commentId));
    }

    @Override
    public void decrementPromptLikeCount(Long promptId) {
        safeDecrement(promptLikeKey(promptId));
    }

    private void safeDecrement(String key) {
        redisTemplate.execute(safeDecrScript, List.of(key));
    }

    @Override
    public Map<Long, Long> getCommentLikeCounts(List<Long> commentIds) {
        return getCounts(commentIds, this::commentLikeKey);
    }

    @Override
    public Map<Long, Long> getPromptLikeCounts(List<Long> promptIds) {
        return getCounts(promptIds, this::promptLikeKey);
    }

    private Map<Long, Long> getCounts(List<Long> ids, Function<Long, String> keyMapper) {
        if (ids == null || ids.isEmpty()) return Map.of();

        List<String> keys = ids.stream().map(keyMapper).toList();
        List<String> valuesRaw = redisTemplate.opsForValue().multiGet(keys);

        // null 체크 및 길이 불일치 대응
        final List<String> values = (valuesRaw == null || valuesRaw.size() != ids.size())
                ? keys.stream().map(k -> "0").toList()
                : valuesRaw;

        Map<Long, Long> result = new HashMap<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            result.put(ids.get(i), safeParse(values.get(i)));
        }
        return result;
    }

    private Long safeParse(String v) {
        if (v == null) return 0L;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
