package org.example.sharedprompts.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CommentCountServiceImpl implements CommentCountService {

    private final StringRedisTemplate redisTemplate;

    private String commentKey(Long promptId) {
        return "comment:prompt:" + promptId;
    }

    private String replyKey(Long parentId) {
        return "comment:parent:" + parentId;
    }

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
        redisTemplate.opsForValue().decrement(commentKey(promptId));
    }

    @Override
    public void decrementReplyCount(Long parentId) {
        redisTemplate.opsForValue().decrement(replyKey(parentId));
    }

    @Override
    public Map<Long, Long> getReplyCounts(List<Long> parentIds) {
        List<String> keys = parentIds.stream()
                .map(id -> "comment:parent:" + id)
                .toList();

        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        return IntStream.range(0, parentIds.size())
                .boxed()
                .collect(Collectors.toMap(
                        parentIds::get,
                        i -> {
                            String v = Objects.requireNonNull(values).get(i);
                            return v != null ? Long.parseLong(v) : 0L;
                        }
                ));
    }

}
