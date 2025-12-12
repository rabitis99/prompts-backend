package org.example.sharedprompts.domain.comment.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.comment.lua.LuaScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentCountServiceImpl implements CommentCountService {

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<Long> safeDecrScript;

    @PostConstruct
    public void init() {
        safeDecrScript = new DefaultRedisScript<>();
        safeDecrScript.setScriptText(LuaScript.SAFE_DECREMENT);
        safeDecrScript.setResultType(Long.class);
    }

    // Redis Key 헬퍼
    private String commentKey(Long promptId) {
        return "comment:prompt:" + promptId;
    }

    private String replyKey(Long parentId) {
        return "comment:parent:" + parentId;
    }

    // ------------------ 증가/감소 ------------------

    @Override
    public void incrementCommentCount(Long promptId) {
        redisTemplate.opsForValue().increment(commentKey(promptId));
    }

    @Override
    public void incrementReplyCount(Long parentId) {
        redisTemplate.opsForValue().increment(replyKey(parentId));
    }

    @Override
    public void decrementCommentCount(Long promptId) {
        safeDecrement(commentKey(promptId));
    }

    @Override
    public void decrementReplyCount(Long parentId) {
        safeDecrement(replyKey(parentId));
    }

    private void safeDecrement(String key) {
        redisTemplate.execute(safeDecrScript, List.of(key));
    }

    // ------------------ 조회 ------------------

    @Override
    public Map<Long, Long> getReplyCounts(List<Long> parentIds) {
        return getCounts(parentIds, this::replyKey);
    }

    @Override
    public Map<Long, Long> getCommentCounts(List<Long> promptIds) {
        return getCounts(promptIds, this::commentKey);
    }

    // 안전한 multiGet + 파싱 + 키 일관성
    private Map<Long, Long> getCounts(List<Long> ids, java.util.function.Function<Long, String> keyMapper) {
        List<String> keys = ids.stream().map(keyMapper).toList();
        List<String> valuesRaw = redisTemplate.opsForValue().multiGet(keys);

        // null 체크 및 길이 불일치 대응
        final List<String> values = (valuesRaw == null || valuesRaw.size() != ids.size())
                ? keys.stream().map(k -> "0").toList()
                : valuesRaw;

        Map<Long, Long> result = new HashMap<>();
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
